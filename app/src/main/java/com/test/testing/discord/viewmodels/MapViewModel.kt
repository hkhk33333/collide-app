package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.data.repository.UserRepositoryImpl
import com.test.testing.discord.domain.usecase.GetUsersUseCase
import com.test.testing.discord.models.*
import com.test.testing.discord.ui.map.MapScreenUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
    private val coroutineManager: CoroutineManager = CoroutineManager(),
) : AndroidViewModel(application),
    DomainEventSubscriber {
    private val userRepositoryImpl = UserRepositoryImpl(application, ApiClient.apiService)
    private val getUsersUseCase = GetUsersUseCase(userRepositoryImpl)
    private val eventBus = SimpleEventBus()

    private val _uiState = MutableStateFlow<MapScreenUiState>(MapScreenUiState.Loading)
    val uiState: StateFlow<MapScreenUiState> = _uiState.asStateFlow()

    // Convenience flow for accessing users list
    val users: StateFlow<List<User>> =
        uiState
            .map { state ->
                when (state) {
                    is MapScreenUiState.Success -> state.users
                    else -> emptyList()
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var refreshJob: Job? = null
    private val refreshInterval = 30000L // 30 seconds

    private val token: String?
        get() =
            AuthManager.instance.token.value
                ?.let { "Bearer $it" }

    init {
        eventBus.subscribe(this)
        loadUsers()
    }

    fun loadUsers() {
        if (token == null) {
            _uiState.value =
                MapScreenUiState.Error(
                    message = "Authentication required. Please log in again.",
                    errorType = com.test.testing.discord.models.ErrorType.AUTHENTICATION,
                )
            return
        }

        _uiState.value = MapScreenUiState.Loading

        coroutineManager.launch {
            getUsersUseCase(token!!).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value =
                            MapScreenUiState.Success(
                                users = result.data,
                                isRefreshing = false,
                            )
                        eventBus.publish(DomainEvent.DataRefreshCompleted(true))
                    }
                    is Result.Error -> {
                        val errorState = mapExceptionToUiState(result.exception)
                        _uiState.value = errorState
                        eventBus.publish(DomainEvent.DataRefreshCompleted(false))
                    }
                }
            }
        }
    }

    fun refreshUsers() {
        val currentState = _uiState.value
        if (currentState is MapScreenUiState.Loading ||
            (currentState is MapScreenUiState.Success && currentState.isRefreshing)
        ) {
            return // Prevent multiple concurrent refreshes
        }

        coroutineManager.launch {
            if (currentState is MapScreenUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            getUsersUseCase(token ?: "").collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value =
                            MapScreenUiState.Success(
                                users = result.data,
                                isRefreshing = false,
                            )
                    }
                    is Result.Error -> {
                        if (currentState is MapScreenUiState.Success) {
                            _uiState.value = currentState.copy(isRefreshing = false)
                        } else {
                            _uiState.value = mapExceptionToUiState(result.exception)
                        }
                    }
                }
            }
        }
    }

    private fun mapExceptionToUiState(exception: Exception): MapScreenUiState.Error =
        when {
            exception.message?.contains("401") == true ||
                exception.message?.contains("403") == true -> {
                MapScreenUiState.Error(
                    message = "Authentication failed. Please log in again.",
                    errorType = com.test.testing.discord.models.ErrorType.AUTHENTICATION,
                )
            }
            exception.message?.contains("5") == true -> {
                MapScreenUiState.Error(
                    message = "Server error occurred. Please try again later.",
                    errorType = com.test.testing.discord.models.ErrorType.SERVER,
                )
            }
            else -> {
                MapScreenUiState.Error(
                    message = exception.message ?: "Network error occurred",
                    errorType = com.test.testing.discord.models.ErrorType.NETWORK,
                )
            }
        }

    fun startPeriodicRefresh() {
        if (refreshJob?.isActive == true) return

        refreshJob =
            coroutineManager.launch {
                while (true) {
                    delay(refreshInterval)
                    refreshUsers()
                }
            }
    }

    fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun initializeForUser(user: User) {
        // Initialize user-specific features
        startPeriodicRefresh()
    }

    fun clearData() {
        stopPeriodicRefresh()
        _uiState.value = MapScreenUiState.Success(emptyList(), false)
    }

    override fun onEvent(event: DomainEvent) {
        when (event) {
            is DomainEvent.UserLoggedOut -> {
                stopPeriodicRefresh()
                _uiState.value =
                    MapScreenUiState.Error(
                        message = "Authentication required. Please log in again.",
                        errorType = com.test.testing.discord.models.ErrorType.AUTHENTICATION,
                    )
            }
            is DomainEvent.DataCleared -> {
                _uiState.value = MapScreenUiState.Success(emptyList(), false)
            }
            else -> {
                // Handle other events if needed
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventBus.unsubscribe(this)
        stopPeriodicRefresh()
    }
}
