package com.test.testing.discord.viewmodels

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.data.repository.UserRepositoryImpl
import com.test.testing.discord.domain.usecase.DeleteUserDataUseCase
import com.test.testing.discord.domain.usecase.GetCurrentUserUseCase
import com.test.testing.discord.domain.usecase.GetGuildsUseCase
import com.test.testing.discord.domain.usecase.GetUsersUseCase
import com.test.testing.discord.domain.usecase.UpdateCurrentUserUseCase
import com.test.testing.discord.domain.usecase.UserUseCases
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.models.*
import com.test.testing.discord.ui.map.MapScreenUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.test.testing.discord.models.Location as ApiLocation

class ApiViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _guilds = MutableStateFlow<List<Guild>>(emptyList())
    val guilds = _guilds.asStateFlow()

    private val _uiState = MutableStateFlow<MapScreenUiState>(MapScreenUiState.Loading)
    val uiState: StateFlow<MapScreenUiState> = _uiState.asStateFlow()

    private val userUseCases: UserUseCases
    private val locationManager = LocationManager.getInstance(application)

    init {
        val userRepository = UserRepositoryImpl(ApiClient.apiService)
        userUseCases =
            UserUseCases(
                getUsers = GetUsersUseCase(userRepository),
                getCurrentUser = GetCurrentUserUseCase(userRepository),
                getGuilds = GetGuildsUseCase(userRepository),
                updateCurrentUser = UpdateCurrentUserUseCase(userRepository),
                deleteUserData = DeleteUserDataUseCase(userRepository),
            )
        loadInitialData()
        observeLocationUpdates()
    }

    private fun observeLocationUpdates() {
        viewModelScope.launch {
            locationManager.locationUpdates.collect { location ->
                location?.let { sendLocationUpdate(it) }
            }
        }
    }

    private fun sendLocationUpdate(location: Location) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                val newLocation =
                    ApiLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy.toDouble(),
                        desiredAccuracy = locationManager.desiredAccuracy.toDouble(),
                        lastUpdated = System.currentTimeMillis().toDouble(),
                    )
                val updatedUser = user.copy(location = newLocation)
                updateCurrentUser(updatedUser) {}
            }
        }
    }

    private var refreshJob: Job? = null
    private val refreshInterval = 30000L // 30 seconds, same as iOS default

    private val token: String?
        get() =
            AuthManager.instance.token.value
                ?.let { "Bearer $it" }

    fun startDataRefresh() {
        if (refreshJob?.isActive == true) return // Don't start if already running
        Log.d("ApiViewModel", "Starting periodic data refresh timer.")
        refreshJob =
            viewModelScope.launch {
                // Start the periodic refresh loop (no initial load here)
                while (true) {
                    delay(refreshInterval)
                    Log.d("ApiViewModel", "Timer triggered. Refreshing user data.")
                    refreshUsers()
                }
            }
    }

    fun stopDataRefresh() {
        Log.d("ApiViewModel", "Stopping periodic data refresh timer.")
        refreshJob?.cancel()
        refreshJob = null
    }

    fun loadInitialData() {
        if (token == null) return

        viewModelScope.launch {
            _uiState.value = MapScreenUiState.Loading

            // Fetch current user and guilds once
            userUseCases.getCurrentUser(token!!).collect { user ->
                _currentUser.value = user
            }
            userUseCases.getGuilds(token!!).collect { guildList ->
                _guilds.value = guildList
            }

            // Fetch initial users data
            userUseCases.getUsers(token!!).collect { userList ->
                _users.value = userList
            }

            // Start periodic user refresh
            startDataRefresh()
            _uiState.value = MapScreenUiState.Success(_users.value, isRefreshing = false)
        }
    }

    // New public function for manual refresh from the UI
    fun manualRefresh() {
        val currentState = _uiState.value
        if (currentState is MapScreenUiState.Loading ||
            (currentState is MapScreenUiState.Success && currentState.isRefreshing)
        ) {
            return // Prevent multiple concurrent refreshes
        }
        viewModelScope.launch {
            Log.d("ApiViewModel", "Manual refresh triggered.")
            refreshUsers()
        }
    }

    // A specific function for refreshing just the users list, which is what the timer does.
    suspend fun refreshUsers() {
        token?.let {
            val currentState = _uiState.value
            if (currentState is MapScreenUiState.Success) {
                // If we're already showing success, just set refreshing flag
                _uiState.value = currentState.copy(isRefreshing = true)
            } else {
                // If we're in a different state, set to loading
                _uiState.value = MapScreenUiState.Loading
            }

            userUseCases
                .getUsers(it)
                .catch { e ->
                    if (currentState is MapScreenUiState.Success) {
                        // If we were in success state, go back to success without refreshing flag
                        _uiState.value = currentState.copy(isRefreshing = false)
                    } else {
                        // Otherwise, set error state
                        _uiState.value = MapScreenUiState.Error("Failed to refresh users: ${e.message}")
                    }
                    Log.e("ApiViewModel", "Error refreshing users", e)
                }.collect { userList ->
                    _users.value = userList
                    _uiState.value = MapScreenUiState.Success(userList, isRefreshing = false)
                }
        }
    }

    fun updateCurrentUser(
        user: User,
        onComplete: () -> Unit,
    ) {
        token?.let {
            viewModelScope.launch {
                _uiState.value = MapScreenUiState.Loading
                try {
                    userUseCases.updateCurrentUser(it, user)
                    _currentUser.value = user
                    refreshUsers()
                } catch (e: Exception) {
                    _uiState.value = MapScreenUiState.Error("Failed to update user: ${e.message}")
                } finally {
                    onComplete()
                }
            }
        }
    }

    fun deleteUserData(onComplete: () -> Unit) {
        token?.let {
            viewModelScope.launch {
                userUseCases.deleteUserData(it)
            }
            onComplete()
        }
    }

    fun clearData() {
        _currentUser.value = null
        _users.value = emptyList()
        _guilds.value = emptyList()
        _uiState.value = MapScreenUiState.Success(emptyList(), isRefreshing = false)
        stopDataRefresh()
    }
}
