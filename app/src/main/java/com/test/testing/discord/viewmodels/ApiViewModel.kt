package com.test.testing.discord.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.models.*
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

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
            _isLoading.value = true
            _error.value = null
            try {
                // Launch concurrently
                val userJob = launch { fetchCurrentUser() }
                val usersJob = launch { fetchUsers() }
                val guildsJob = launch { fetchGuilds() }

                userJob.join()
                usersJob.join()
                guildsJob.join()
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
                Log.e("ApiViewModel", "Error loading initial data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // New public function for manual refresh from the UI
    fun manualRefresh() {
        if (_isLoading.value) return // Prevent multiple concurrent refreshes
        viewModelScope.launch {
            Log.d("ApiViewModel", "Manual refresh triggered.")
            refreshUsers()
        }
    }

    // A specific function for refreshing just the users list, which is what the timer does.
    suspend fun refreshUsers() {
        if (token == null) return
        _isLoading.value = true
        try {
            fetchUsers()
        } catch (e: Exception) {
            _error.value = "Failed to refresh users: ${e.message}"
            Log.e("ApiViewModel", "Error refreshing users", e)
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun fetchCurrentUser() {
        token?.let {
            val response = ApiClient.apiService.getCurrentUser(it)
            if (response.isSuccessful) {
                _currentUser.value = response.body()
            } else {
                handleApiError("fetchCurrentUser", response.code())
            }
        }
    }

    private suspend fun fetchUsers() {
        token?.let {
            val response = ApiClient.apiService.getUsers(it)
            if (response.isSuccessful) {
                _users.value = response.body() ?: emptyList()
            } else {
                handleApiError("fetchUsers", response.code())
            }
        }
    }

    private suspend fun fetchGuilds() {
        token?.let {
            val response = ApiClient.apiService.getGuilds(it)
            if (response.isSuccessful) {
                _guilds.value = response.body() ?: emptyList()
            } else {
                handleApiError("fetchGuilds", response.code())
            }
        }
    }

    fun updateCurrentUser(
        user: User,
        onComplete: () -> Unit,
    ) {
        token?.let {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val response = ApiClient.apiService.updateCurrentUser(it, user)
                    if (response.isSuccessful) {
                        _currentUser.value = user
                        // Refresh users list as privacy settings might have changed
                        fetchUsers()
                    } else {
                        handleApiError("updateCurrentUser", response.code())
                    }
                } catch (e: Exception) {
                    _error.value = "Failed to update user: ${e.message}"
                } finally {
                    _isLoading.value = false
                    onComplete()
                }
            }
        }
    }

    fun deleteUserData(onComplete: () -> Unit) {
        token?.let {
            viewModelScope.launch {
                try {
                    ApiClient.apiService.deleteUserData(it)
                } catch (e: Exception) {
                    Log.e("ApiViewModel", "Failed to delete user data", e)
                } finally {
                    onComplete()
                }
            }
        }
    }

    private fun handleApiError(
        source: String,
        code: Int,
    ) {
        val errorMessage = "API Error in $source: Code $code"
        Log.e("ApiViewModel", errorMessage)
        _error.value = errorMessage
    }

    fun clearData() {
        _currentUser.value = null
        _users.value = emptyList()
        _guilds.value = emptyList()
        _error.value = null
        stopDataRefresh()
    }
}
