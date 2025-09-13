package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.domain.usecase.*
import com.test.testing.discord.models.*
import com.test.testing.discord.ui.settings.UserEffect
import com.test.testing.discord.ui.settings.UserIntent
import com.test.testing.discord.ui.settings.UserScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel
    @Inject
    constructor(
        application: Application,
        private val savedStateHandle: SavedStateHandle,
        private val getCurrentUserUseCase: GetCurrentUserUseCase,
        private val getGuildsUseCase: GetGuildsUseCase,
        private val updateUserUseCase: UpdateCurrentUserUseCase,
        private val deleteUserDataUseCase: DeleteUserDataUseCase,
        private val authManager: AuthManager,
        private val eventBus: DomainEventBus,
    ) : AndroidViewModel(application),
        DomainEventSubscriber {
        companion object {
            private const val KEY_USER_STATE = "user_screen_state"
        }

        private val _state =
            MutableStateFlow(
                savedStateHandle.get<UserScreenState>(KEY_USER_STATE) ?: UserScreenState.Loading,
            )
        val state: StateFlow<UserScreenState> = _state.asStateFlow()

        private val _effects = Channel<UserEffect>()
        val effects = _effects.receiveAsFlow()

        // Computed properties for backward compatibility
        val currentUser: StateFlow<com.test.testing.discord.domain.models.DomainUser?> =
            state
                .map { state ->
                    when (state) {
                        is UserScreenState.Content -> state.currentUser
                        else -> null
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, null)

        val guilds: StateFlow<List<com.test.testing.discord.domain.models.DomainGuild>> =
            state
                .map { state ->
                    when (state) {
                        is UserScreenState.Content -> state.guilds
                        else -> emptyList()
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        val isLoading: StateFlow<Boolean> =
            state
                .map { it.isLoading }
                .stateIn(viewModelScope, SharingStarted.Lazily, false)

        private val token: String?
            get() =
                authManager.token.value
                    ?.let { "Bearer $it" }

        private val exceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                val currentState = _state.value
                when (currentState) {
                    is UserScreenState.Content -> {
                        _state.value = currentState.copy()
                    }

                    is UserScreenState.Error -> {
                        _state.value = currentState.copy()
                    }

                    else -> {
                        _state.value =
                            UserScreenState.Error(
                                message = throwable.message ?: "An unknown error occurred",
                            )
                    }
                }
            }

        init {
            eventBus.subscribe(this)
            // Save state whenever it changes
            viewModelScope.launch {
                state.collect { currentState ->
                    savedStateHandle[KEY_USER_STATE] = currentState
                }
            }
            loadInitialData()
        }

        // Process user intents following MVI pattern
        fun processIntent(intent: UserIntent) {
            when (intent) {
                is UserIntent.UpdateUser -> {
                    // For user updates, we need to call the actual update method
                    // which handles the server call and state management
                    updateCurrentUser(intent.user) { result ->
                        // Handle completion if needed
                        when (result) {
                            is Result.Success -> {
                                // Update was successful, state should already be updated by updateCurrentUser
                            }

                            is Result.Error -> {
                                // Handle error if needed
                            }
                        }
                    }
                }

                else -> {
                    val currentState = _state.value
                    val newState = reduce(currentState, intent)
                    _state.value = newState
                }
            }
        }

        // Reducer function for state transformations
        private fun reduce(
            state: UserScreenState,
            intent: UserIntent,
        ): UserScreenState =
            when (intent) {
                is UserIntent.LoadData -> {
                    UserScreenState.Loading
                }

                is UserIntent.UpdateUser -> {
                    // For update operations, we keep the current state but set loading
                    when (state) {
                        is UserScreenState.Content -> state.copy()
                        else -> state
                    }
                }

                is UserIntent.DeleteUserData -> {
                    // For delete operations, we keep the current state but set loading
                    when (state) {
                        is UserScreenState.Content -> state.copy()
                        else -> state
                    }
                }
            }

        private fun loadInitialData() {
            if (token == null) {
                _state.value =
                    UserScreenState.Error(
                        message = "Authentication required. Please log in again.",
                        errorType = ErrorType.AUTHENTICATION,
                    )
                return
            }

            // State is already set to Loading by the reducer when LoadData intent is processed

            viewModelScope.launch(exceptionHandler) {
                try {
                    var currentUserData: com.test.testing.discord.domain.models.DomainUser? = null
                    var guildsData: List<com.test.testing.discord.domain.models.DomainGuild> = emptyList()

                    // Load current user
                    getCurrentUserUseCase(token!!).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                result.data?.let {
                                    currentUserData = it
                                    eventBus.publish(DomainEvent.UserDataUpdated(it))
                                }
                            }

                            is Result.Error -> {
                                eventBus.publish(DomainEvent.NetworkError("getCurrentUser", result.exception))
                            }
                        }
                    }

                    // Load guilds
                    getGuildsUseCase(token!!).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                guildsData = result.data
                            }

                            is Result.Error -> {
                                eventBus.publish(DomainEvent.NetworkError("getGuilds", result.exception))
                            }
                        }
                    }

                    // Set final state
                    _state.value =
                        UserScreenState.Content(
                            currentUser = currentUserData,
                            guilds = guildsData,
                        )
                } catch (e: Exception) {
                    _state.value =
                        UserScreenState.Error(
                            message = e.message ?: "Failed to load user data",
                            errorType = ErrorType.UNKNOWN,
                        )
                }
            }
        }

        fun updateCurrentUser(
            user: com.test.testing.discord.domain.models.DomainUser,
            onComplete: (Result<Unit>) -> Unit = {},
        ) {
            if (token == null) {
                onComplete(Result.error("No authentication token available"))
                return
            }

            // State loading is set by the reducer when UpdateUser intent is processed

            viewModelScope.launch(exceptionHandler) {
                try {
                    val result = updateUserUseCase(token!!, user)
                    when (result) {
                        is Result.Success -> {
                            val currentState = _state.value
                            val updatedState =
                                if (currentState is UserScreenState.Content) {
                                    currentState.copy(
                                        currentUser = user,
                                    )
                                } else {
                                    UserScreenState.Content(
                                        currentUser = user,
                                        guilds = emptyList(),
                                    )
                                }
                            _state.value = updatedState
                            eventBus.publish(DomainEvent.UserDataUpdated(user))
                            onComplete(result)
                        }

                        is Result.Error -> {
                            val errorState =
                                UserScreenState.Error(
                                    message = result.exception.message ?: "Failed to update user",
                                    errorType = ErrorType.UNKNOWN,
                                    canRetry = true,
                                )
                            _state.value = errorState
                            eventBus.publish(DomainEvent.NetworkError("updateCurrentUser", result.exception))
                            onComplete(result)
                        }
                    }
                } catch (e: Exception) {
                    val errorState =
                        UserScreenState.Error(
                            message = e.message ?: "Failed to update user",
                            errorType = ErrorType.UNKNOWN,
                            canRetry = true,
                        )
                    _state.value = errorState
                }
            }
        }

        fun deleteUserData(onComplete: (Result<Unit>) -> Unit = {}) {
            if (token == null) {
                onComplete(Result.error("No authentication token available"))
                return
            }

            // State loading is set by the reducer when DeleteUserData intent is processed

            viewModelScope.launch(exceptionHandler) {
                try {
                    val result = deleteUserDataUseCase(token!!)
                    when (result) {
                        is Result.Success -> {
                            eventBus.publish(DomainEvent.DataCleared)
                            _state.value =
                                UserScreenState.Content(
                                    currentUser = null,
                                    guilds = emptyList(),
                                )
                            onComplete(result)
                        }

                        is Result.Error -> {
                            val errorState =
                                UserScreenState.Error(
                                    message = result.exception.message ?: "Failed to delete user data",
                                    errorType = ErrorType.UNKNOWN,
                                    canRetry = true,
                                )
                            _state.value = errorState
                            eventBus.publish(DomainEvent.NetworkError("deleteUserData", result.exception))
                            onComplete(result)
                        }
                    }
                } catch (e: Exception) {
                    val errorState =
                        UserScreenState.Error(
                            message = e.message ?: "Failed to delete user data",
                            errorType = ErrorType.UNKNOWN,
                            canRetry = true,
                        )
                    _state.value = errorState
                }
            }
        }

        fun logout(onComplete: (() -> Unit)? = null) {
            authManager.logout(onComplete ?: {})
        }

        fun clearData() {
            _state.value =
                UserScreenState.Content(
                    currentUser = null,
                    guilds = emptyList(),
                )
        }

        override fun onEvent(event: DomainEvent) {
            when (event) {
                is DomainEvent.UserLoggedOut -> {
                    clearData()
                }

                is DomainEvent.DataCleared -> {
                    clearData()
                }

                else -> {
                    // Handle other events if needed
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            eventBus.unsubscribe(this)
        }
    }
