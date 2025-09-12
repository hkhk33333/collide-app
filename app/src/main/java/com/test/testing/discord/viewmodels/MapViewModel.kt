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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
) : AndroidViewModel(application),
    DomainEventSubscriber {
    private val userRepositoryImpl = UserRepositoryImpl(application, ApiClient.apiService)
    private val getUsersUseCase = GetUsersUseCase(userRepositoryImpl)
    private val eventBus = SimpleEventBus()

    private val _uiState = MutableStateFlow<MapScreenUiState>(MapScreenUiState.Loading)
    val uiState: StateFlow<MapScreenUiState> = _uiState.asStateFlow()

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

    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            _uiState.value = mapExceptionToUiState(throwable as? Exception ?: Exception("Unknown error"))
        }

    init {
        eventBus.subscribe(this)
        loadUsers()
    }

    // Handle UI events
    fun onEvent(event: com.test.testing.discord.ui.UiEvent) {
        when (event) {
            is com.test.testing.discord.ui.UiEvent.RefreshUsers -> refreshUsers()
            is com.test.testing.discord.ui.UiEvent.LoadUsers -> loadUsers()
            else -> {
                // Handle other events if needed
            }
        }
    }

    private fun loadUsers() {
        if (token == null) {
            _uiState.value =
                MapScreenUiState.Error(
                    message = "Authentication required. Please log in again.",
                    errorType = com.test.testing.discord.models.ErrorType.AUTHENTICATION,
                )
            return
        }

        _uiState.value = MapScreenUiState.Loading

        viewModelScope.launch(exceptionHandler) {
            getUsersUseCase(token!!, forceRefresh = false).collect { result ->
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
                        _uiState.value = mapExceptionToUiState(result.exception)
                        eventBus.publish(DomainEvent.DataRefreshCompleted(false))
                    }
                }
            }
        }
    }

    fun refreshUsers() {
        val currentState = _uiState.value
        android.util.Log.d("MapViewModel", "refreshUsers called, current state: $currentState")

        if (currentState is MapScreenUiState.Loading ||
            currentState.isRefreshing
        ) {
            android.util.Log.d("MapViewModel", "Refresh prevented - already loading or refreshing")
            return // Prevent multiple concurrent refreshes
        }

        if (token == null) {
            android.util.Log.d("MapViewModel", "No token available")
            _uiState.value =
                MapScreenUiState.Error(
                    message = "Authentication required. Please log in again.",
                    errorType = com.test.testing.discord.models.ErrorType.AUTHENTICATION,
                    isRefreshing = false,
                )
            return
        }

        android.util.Log.d("MapViewModel", "Starting refresh with token")
        viewModelScope.launch(exceptionHandler) {
            // Set refreshing state for both Success and Error states
            when (currentState) {
                is MapScreenUiState.Success -> {
                    _uiState.value = currentState.copy(isRefreshing = true)
                    android.util.Log.d("MapViewModel", "Set Success state to refreshing")
                }
                is MapScreenUiState.Error -> {
                    _uiState.value = currentState.copy(isRefreshing = true)
                    android.util.Log.d("MapViewModel", "Set Error state to refreshing")
                }
                else -> {
                    android.util.Log.d("MapViewModel", "Current state is Loading, not changing")
                    // For Loading state, we don't change it
                }
            }

            android.util.Log.d("MapViewModel", "Calling getUsersUseCase with forceRefresh=true")
            getUsersUseCase(token!!, forceRefresh = true).collect { result ->
                android.util.Log.d("MapViewModel", "Received result: $result")
                when (result) {
                    is Result.Success -> {
                        _uiState.value =
                            MapScreenUiState.Success(
                                users = result.data,
                                isRefreshing = false,
                            )
                        android.util.Log.d("MapViewModel", "Set state to Success with ${result.data.size} users")
                    }
                    is Result.Error -> {
                        _uiState.value = mapExceptionToUiState(result.exception)
                        android.util.Log.d("MapViewModel", "Set state to Error: ${result.exception.message}")
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
                    isRefreshing = false,
                )
            }
            exception.message?.contains("5") == true -> {
                MapScreenUiState.Error(
                    message = "Server error occurred. Please try again later.",
                    errorType = com.test.testing.discord.models.ErrorType.SERVER,
                    isRefreshing = false,
                )
            }
            else -> {
                MapScreenUiState.Error(
                    message = exception.message ?: "Network error occurred",
                    errorType = com.test.testing.discord.models.ErrorType.NETWORK,
                    isRefreshing = false,
                )
            }
        }

    fun startPeriodicRefresh() {
        if (refreshJob?.isActive == true) return

        refreshJob =
            viewModelScope.launch(exceptionHandler) {
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

    fun initializeForUser() {
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
