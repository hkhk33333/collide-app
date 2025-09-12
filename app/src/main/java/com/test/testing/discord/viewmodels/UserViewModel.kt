package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.data.repository.UserRepositoryImpl
import com.test.testing.discord.domain.usecase.*
import com.test.testing.discord.models.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserViewModel(
    application: Application,
) : AndroidViewModel(application),
    DomainEventSubscriber {
    private val userRepositoryImpl = UserRepositoryImpl(application, ApiClient.apiService)
    private val getCurrentUserUseCase = GetCurrentUserUseCase(userRepositoryImpl)
    private val getGuildsUseCase = GetGuildsUseCase(userRepositoryImpl)
    private val updateUserUseCase = UpdateCurrentUserUseCase(userRepositoryImpl)
    private val deleteUserDataUseCase = DeleteUserDataUseCase(userRepositoryImpl)
    private val eventBus = SimpleEventBus()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _guilds = MutableStateFlow<List<Guild>>(emptyList())
    val guilds: StateFlow<List<Guild>> = _guilds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val token: String?
        get() =
            AuthManager.instance.token.value
                ?.let { "Bearer $it" }

    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            _error.value = throwable.message ?: "An unknown error occurred"
            _isLoading.value = false
        }

    init {
        eventBus.subscribe(this)
        loadInitialData()
    }

    private fun loadInitialData() {
        if (token == null) return

        _isLoading.value = true

        viewModelScope.launch(exceptionHandler) {
            try {
                // Load current user
                getCurrentUserUseCase(token!!).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            result.data?.let {
                                _currentUser.value = it
                                eventBus.publish(DomainEvent.UserDataUpdated(it))
                            }
                        }
                        is Result.Error -> {
                            _error.value = result.exception.message
                            eventBus.publish(DomainEvent.NetworkError("getCurrentUser", result.exception))
                        }
                    }
                }

                // Load guilds
                getGuildsUseCase(token!!).collect { result ->
                    when (result) {
                        is Result.Success -> _guilds.value = result.data

                        is Result.Error -> {
                            _error.value = result.exception.message
                            eventBus.publish(DomainEvent.NetworkError("getGuilds", result.exception))
                        }
                    }
                }
            } finally {
                _isLoading.value = false
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

        _isLoading.value = true

        viewModelScope.launch(exceptionHandler) {
            try {
                val result = updateUserUseCase(token!!, user)
                when (result) {
                    is Result.Success -> {
                        _currentUser.value = user
                        eventBus.publish(DomainEvent.UserDataUpdated(user))
                        onComplete(result)
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message
                        eventBus.publish(DomainEvent.NetworkError("updateCurrentUser", result.exception))
                        onComplete(result)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUserData(onComplete: (Result<Unit>) -> Unit = {}) {
        if (token == null) {
            onComplete(Result.error("No authentication token available"))
            return
        }

        _isLoading.value = true

        viewModelScope.launch(exceptionHandler) {
            try {
                val result = deleteUserDataUseCase(token!!)
                when (result) {
                    is Result.Success -> {
                        eventBus.publish(DomainEvent.DataCleared)
                        onComplete(result)
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message
                        eventBus.publish(DomainEvent.NetworkError("deleteUserData", result.exception))
                        onComplete(result)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearData() {
        _currentUser.value = null
        _guilds.value = emptyList()
        _isLoading.value = false
        _error.value = null
    }

    override fun onEvent(event: DomainEvent) {
        when (event) {
            is DomainEvent.UserLoggedOut -> clearData()
            is DomainEvent.DataCleared -> clearData()
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
