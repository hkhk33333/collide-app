package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.BuildConfig
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
    private val userRepositoryImpl = UserRepositoryImpl(application, ApiClient.getInstance().apiService)
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
        if (!canRefreshUsers()) return

        if (BuildConfig.DEBUG) {
            android.util.Log.d("MapViewModel", "Starting refresh with token")
        }

        viewModelScope.launch(exceptionHandler) {
            setRefreshingState()
            performUserRefresh()
        }
    }

    private fun canRefreshUsers(): Boolean {
        val currentState = _uiState.value

        if (BuildConfig.DEBUG) {
            android.util.Log.d("MapViewModel", "refreshUsers called, current state: $currentState")
        }

        return when {
            currentState is MapScreenUiState.Loading || currentState.isRefreshing -> {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("MapViewModel", "Refresh prevented - already loading or refreshing")
                }

                false
            }
            token == null -> {
                handleMissingToken()
                false
            }
            else -> true
        }
    }

    private fun handleMissingToken() {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("MapViewModel", "No token available")
        }
        _uiState.value =
            MapScreenUiState.Error(
                message = "Authentication required. Please log in again.",
                errorType = com.test.testing.discord.models.ErrorType.AUTHENTICATION,
                isRefreshing = false,
            )
    }

    private fun setRefreshingState() {
        val currentState = _uiState.value
        _uiState.value =
            when (currentState) {
                is MapScreenUiState.Success -> currentState.copy(isRefreshing = true)
                is MapScreenUiState.Error -> currentState.copy(isRefreshing = true)
                else -> currentState // Loading state remains unchanged
            }

        if (BuildConfig.DEBUG) {
            android.util.Log.d("MapViewModel", "Set state to refreshing")
        }
    }

    private suspend fun performUserRefresh() {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("MapViewModel", "Calling getUsersUseCase with forceRefresh=true")
        }

        getUsersUseCase(token!!, forceRefresh = true).collect { result ->
            handleRefreshResult(result)
        }
    }

    private fun handleRefreshResult(result: Result<List<com.test.testing.discord.models.User>>) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("MapViewModel", "Received result: $result")
        }

        _uiState.value =
            when (result) {
                is Result.Success -> {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MapViewModel", "Set state to Success with ${result.data.size} users")
                    }
                    MapScreenUiState.Success(
                        users = result.data,
                        isRefreshing = false,
                    )
                }

                is Result.Error -> {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MapViewModel", "Set state to Error: ${result.exception.message}")
                    }
                    mapExceptionToUiState(result.exception)
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
