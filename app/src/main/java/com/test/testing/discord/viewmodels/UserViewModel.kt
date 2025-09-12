package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.domain.usecase.*
import com.test.testing.discord.models.*
import com.test.testing.discord.ui.settings.UserScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel
    @Inject
    constructor(
        application: Application,
        private val getCurrentUserUseCase: GetCurrentUserUseCase,
        private val getGuildsUseCase: GetGuildsUseCase,
        private val updateUserUseCase: UpdateCurrentUserUseCase,
        private val deleteUserDataUseCase: DeleteUserDataUseCase,
        private val authManager: AuthManager,
        private val eventBus: DomainEventBus,
    ) : AndroidViewModel(application),
        DomainEventSubscriber {
        private val _uiState = MutableStateFlow<UserScreenUiState>(UserScreenUiState.Loading)
        val uiState: StateFlow<UserScreenUiState> = _uiState.asStateFlow()

        // Computed properties for backward compatibility
        val currentUser: StateFlow<User?> =
            uiState
                .map { state -> state.currentUser }
                .stateIn(viewModelScope, SharingStarted.Lazily, null)

        val guilds: StateFlow<List<Guild>> =
            uiState
                .map { state -> state.guilds }
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        val isLoading: StateFlow<Boolean> =
            uiState
                .map { state -> state.isLoading }
                .stateIn(viewModelScope, SharingStarted.Lazily, false)

        private val token: String?
            get() =
                authManager.token.value
                    ?.let { "Bearer $it" }

        private val exceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                val currentState = _uiState.value
                when (currentState) {
                    is UserScreenUiState.Success -> {
                        _uiState.value = currentState.copy(isLoading = false)
                    }

                    is UserScreenUiState.Error -> {
                        _uiState.value = currentState.copy(isLoading = false)
                    }

                    else -> {
                        _uiState.value =
                            UserScreenUiState.Error(
                                message = throwable.message ?: "An unknown error occurred",
                                isLoading = false,
                            )
                    }
                }
            }

        init {
            eventBus.subscribe(this)
            loadInitialData()
        }

        // Handle UI events
        fun onEvent(event: com.test.testing.discord.ui.UiEvent) {
            when (event) {
                is com.test.testing.discord.ui.UiEvent.UpdateUser -> updateCurrentUser(event.user) {}
                is com.test.testing.discord.ui.UiEvent.DeleteUserData -> deleteUserData {}
                else -> {
                    // Handle other events if needed
                }
            }
        }

        private fun loadInitialData() {
            if (token == null) {
                _uiState.value =
                    UserScreenUiState.Error(
                        message = "Authentication required. Please log in again.",
                        errorType = ErrorType.AUTHENTICATION,
                    )
                return
            }

            _uiState.value = UserScreenUiState.Loading

            viewModelScope.launch(exceptionHandler) {
                try {
                    var currentUserData: User? = null
                    var guildsData: List<Guild> = emptyList()

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
                    _uiState.value =
                        UserScreenUiState.Success(
                            currentUser = currentUserData,
                            guilds = guildsData,
                            isLoading = false,
                        )
                } catch (e: Exception) {
                    _uiState.value =
                        UserScreenUiState.Error(
                            message = e.message ?: "Failed to load user data",
                            errorType = ErrorType.UNKNOWN,
                            isLoading = false,
                        )
                }
            }
        }

        fun updateCurrentUser(
            user: User,
            onComplete: (Result<Unit>) -> Unit = {},
        ) {
            if (token == null) {
                onComplete(Result.error("No authentication token available"))
                return
            }

            val currentState = _uiState.value
            when (currentState) {
                is UserScreenUiState.Success -> {
                    _uiState.value = currentState.copy(isLoading = true)
                }
                else -> {
                    _uiState.value = UserScreenUiState.Loading
                }
            }

            viewModelScope.launch(exceptionHandler) {
                try {
                    val result = updateUserUseCase(token!!, user)
                    when (result) {
                        is Result.Success -> {
                            val updatedState =
                                (_uiState.value as? UserScreenUiState.Success)?.copy(
                                    currentUser = user,
                                    isLoading = false,
                                ) ?: UserScreenUiState.Success(
                                    currentUser = user,
                                    guilds = emptyList(),
                                    isLoading = false,
                                )
                            _uiState.value = updatedState
                            eventBus.publish(DomainEvent.UserDataUpdated(user))
                            onComplete(result)
                        }

                        is Result.Error -> {
                            val errorState =
                                UserScreenUiState.Error(
                                    message = result.exception.message ?: "Failed to update user",
                                    errorType = ErrorType.UNKNOWN,
                                    canRetry = true,
                                    isLoading = false,
                                )
                            _uiState.value = errorState
                            eventBus.publish(DomainEvent.NetworkError("updateCurrentUser", result.exception))
                            onComplete(result)
                        }
                    }
                } catch (e: Exception) {
                    val errorState =
                        UserScreenUiState.Error(
                            message = e.message ?: "Failed to update user",
                            errorType = ErrorType.UNKNOWN,
                            canRetry = true,
                            isLoading = false,
                        )
                    _uiState.value = errorState
                }
            }
        }

        fun deleteUserData(onComplete: (Result<Unit>) -> Unit = {}) {
            if (token == null) {
                onComplete(Result.error("No authentication token available"))
                return
            }

            val currentState = _uiState.value
            when (currentState) {
                is UserScreenUiState.Success -> {
                    _uiState.value = currentState.copy(isLoading = true)
                }
                else -> {
                    _uiState.value = UserScreenUiState.Loading
                }
            }

            viewModelScope.launch(exceptionHandler) {
                try {
                    val result = deleteUserDataUseCase(token!!)
                    when (result) {
                        is Result.Success -> {
                            eventBus.publish(DomainEvent.DataCleared)
                            _uiState.value =
                                UserScreenUiState.Success(
                                    currentUser = null,
                                    guilds = emptyList(),
                                    isLoading = false,
                                )
                            onComplete(result)
                        }

                        is Result.Error -> {
                            val errorState =
                                UserScreenUiState.Error(
                                    message = result.exception.message ?: "Failed to delete user data",
                                    errorType = ErrorType.UNKNOWN,
                                    canRetry = true,
                                    isLoading = false,
                                )
                            _uiState.value = errorState
                            eventBus.publish(DomainEvent.NetworkError("deleteUserData", result.exception))
                            onComplete(result)
                        }
                    }
                } catch (e: Exception) {
                    val errorState =
                        UserScreenUiState.Error(
                            message = e.message ?: "Failed to delete user data",
                            errorType = ErrorType.UNKNOWN,
                            canRetry = true,
                            isLoading = false,
                        )
                    _uiState.value = errorState
                }
            }
        }

        fun logout(onComplete: (() -> Unit)? = null) {
            authManager.logout(onComplete ?: {})
        }

        fun clearData() {
            _uiState.value =
                UserScreenUiState.Success(
                    currentUser = null,
                    guilds = emptyList(),
                    isLoading = false,
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
