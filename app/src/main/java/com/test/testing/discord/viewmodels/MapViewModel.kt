package com.test.testing.discord.viewmodels

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.data.repository.UserRepositoryImpl
import com.test.testing.discord.domain.usecase.GetUsersUseCase
import com.test.testing.discord.domain.usecase.UpdateCurrentUserUseCase
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.models.*
import com.test.testing.discord.ui.map.MapScreenUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
    private val coroutineManager: CoroutineManager = CoroutineManager(),
    private val userRepositoryImpl: UserRepositoryImpl = UserRepositoryImpl(ApiClient.apiService),
) : AndroidViewModel(application),
    DomainEventSubscriber {
    private val getUsersUseCase = GetUsersUseCase(userRepositoryImpl)
    private val updateUserUseCase = UpdateCurrentUserUseCase(userRepositoryImpl)
    private val locationManager = LocationManager.getInstance(application)
    private val eventBus = SimpleEventBus()

    private val _uiState = MutableStateFlow<MapScreenUiState>(MapScreenUiState.Loading)
    val uiState: StateFlow<MapScreenUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private val refreshInterval = 30000L // 30 seconds

    private val token: String?
        get() =
            AuthManager.instance.token.value
                ?.let { "Bearer $it" }

    init {
        eventBus.subscribe(this)
        observeLocationUpdates()
        loadUsers()
    }

    private fun observeLocationUpdates() {
        coroutineManager.launch {
            locationManager.locationUpdates.collect { location ->
                location?.let { sendLocationUpdate(it) }
            }
        }
    }

    private fun sendLocationUpdate(location: Location) {
        coroutineManager.launch {
            val currentUser = getCurrentUser()
            currentUser?.let { user ->
                val newLocation =
                    com.test.testing.discord.models.Location(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy.toDouble(),
                        desiredAccuracy = locationManager.desiredAccuracy.toDouble(),
                        lastUpdated = System.currentTimeMillis().toDouble(),
                    )
                val updatedUser = user.copy(location = newLocation)
                updateUser(updatedUser)
            }
        }
    }

    private suspend fun getCurrentUser(): User? {
        // This would need to be implemented - for now return null
        // In a real implementation, you'd have a separate UserViewModel
        return null // TODO: Implement proper current user retrieval
    }

    fun loadUsers() {
        if (token == null) {
            _uiState.value = MapScreenUiState.Error.AuthenticationError()
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

    private suspend fun updateUser(user: User) {
        token?.let {
            val result = updateUserUseCase(it, user)
            when (result) {
                is Result.Success -> {
                    eventBus.publish(DomainEvent.UserDataUpdated(user))
                }
                is Result.Error -> {
                    eventBus.publish(DomainEvent.NetworkError("updateUser", result.exception))
                }
            }
        }
    }

    private fun mapExceptionToUiState(exception: Exception): MapScreenUiState.Error =
        when {
            exception.message?.contains("401") == true ||
                exception.message?.contains("403") == true -> {
                MapScreenUiState.Error.AuthenticationError()
            }
            exception.message?.contains("5") == true -> {
                MapScreenUiState.Error.ServerError(
                    message = "Server error occurred",
                    code = 500,
                )
            }
            else -> {
                MapScreenUiState.Error.NetworkError(
                    message = exception.message ?: "Network error occurred",
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

    override fun onEvent(event: DomainEvent) {
        when (event) {
            is DomainEvent.UserLoggedOut -> {
                stopPeriodicRefresh()
                _uiState.value = MapScreenUiState.Error.AuthenticationError()
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
