package com.test.testing.discord.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
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
import com.test.testing.discord.models.*
import com.test.testing.discord.ui.map.MapScreenUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ApiViewModel : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _guilds = MutableStateFlow<List<Guild>>(emptyList())
    val guilds = _guilds.asStateFlow()

    private val _uiState = MutableStateFlow(MapScreenUiState())
    val uiState: StateFlow<MapScreenUiState> = _uiState.asStateFlow()

    private val userUseCases: UserUseCases

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
                // Perform an initial load immediately
                loadInitialData()
                // Then start the periodic refresh
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
            _uiState.update { it.copy(isLoading = true) }

            // Fetch current user and guilds once
            userUseCases.getCurrentUser(token!!).collect { user ->
                _currentUser.value = user
            }
            userUseCases.getGuilds(token!!).collect { guildList ->
                _guilds.value = guildList
            }

            // Start periodic user refresh
            startDataRefresh()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // New public function for manual refresh from the UI
    fun manualRefresh() {
        if (_uiState.value.isLoading) return // Prevent multiple concurrent refreshes
        viewModelScope.launch {
            Log.d("ApiViewModel", "Manual refresh triggered.")
            refreshUsers()
        }
    }

    // A specific function for refreshing just the users list, which is what the timer does.
    suspend fun refreshUsers() {
        token?.let {
            _uiState.update { it.copy(isLoading = true) }
            userUseCases
                .getUsers(it)
                .catch { e ->
                    _uiState.update { state ->
                        state.copy(error = "Failed to refresh users: ${e.message}")
                    }
                    Log.e("ApiViewModel", "Error refreshing users", e)
                }.collect { userList ->
                    _users.value = userList
                    _uiState.update { state -> state.copy(users = userList, isLoading = false) }
                }
        }
    }

    fun updateCurrentUser(
        user: User,
        onComplete: () -> Unit,
    ) {
        token?.let {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    userUseCases.updateCurrentUser(it, user)
                    _currentUser.value = user
                    refreshUsers()
                } catch (e: Exception) {
                    _uiState.update { state -> state.copy(error = "Failed to update user: ${e.message}") }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
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
        _uiState.update {
            it.copy(isLoading = false, users = emptyList(), error = null)
        }
        stopDataRefresh()
    }
}
