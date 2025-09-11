package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.data.repository.UserRepositoryImpl
import com.test.testing.discord.domain.usecase.*
import com.test.testing.discord.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserViewModel(
    application: Application,
    private val coroutineManager: CoroutineManager = CoroutineManager(),
    private val userRepositoryImpl: UserRepositoryImpl = UserRepositoryImpl(ApiClient.apiService),
) : AndroidViewModel(application),
    DomainEventSubscriber {
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

    private val token: String?
        get() =
            AuthManager.instance.token.value
                ?.let { "Bearer $it" }

    init {
        eventBus.subscribe(this)
        loadInitialData()
    }

    private fun loadInitialData() {
        if (token == null) return

        _isLoading.value = true

        coroutineManager.launch {
            try {
                // Load current user
                getCurrentUserUseCase(token!!).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _currentUser.value = result.data
                            eventBus.publish(DomainEvent.UserDataUpdated(result.data!!))
                        }
                        is Result.Error -> {
                            eventBus.publish(DomainEvent.NetworkError("getCurrentUser", result.exception))
                        }
                    }
                }

                // Load guilds
                getGuildsUseCase(token!!).collect { result ->
                    when (result) {
                        is Result.Success -> _guilds.value = result.data

                        is Result.Error -> {
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

        coroutineManager.launch {
            try {
                val result = updateUserUseCase(token!!, user)
                when (result) {
                    is Result.Success -> {
                        _currentUser.value = user
                        eventBus.publish(DomainEvent.UserDataUpdated(user))
                        onComplete(result)
                    }
                    is Result.Error -> {
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

        coroutineManager.launch {
            try {
                val result = deleteUserDataUseCase(token!!)
                when (result) {
                    is Result.Success -> {
                        eventBus.publish(DomainEvent.DataCleared)
                        onComplete(result)
                    }
                    is Result.Error -> {
                        eventBus.publish(DomainEvent.NetworkError("deleteUserData", result.exception))
                        onComplete(result)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        coroutineManager.launch {
            AuthManager.instance.logout {
                clearData()
                eventBus.publish(DomainEvent.UserLoggedOut)
                onComplete()
            }
        }
    }

    private fun clearData() {
        _currentUser.value = null
        _guilds.value = emptyList()
        _isLoading.value = false
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
