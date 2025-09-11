package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Main App Coordinator ViewModel
 *
 * Responsibilities:
 * - Coordinate between feature ViewModels
 * - Manage app-level state
 * - Handle cross-feature communication
 */
class AppViewModel(
    application: Application,
) : AndroidViewModel(application) {
    // Feature ViewModels
    val authViewModel = AuthViewModel(application)
    val mapViewModel = MapViewModel(application)
    val userViewModel = UserViewModel(application)

    // Convenience flows for UI consumption
    val currentUser = userViewModel.currentUser
    val users = mapViewModel.users
    val guilds = userViewModel.guilds

    init {
        observeUserState()
        observeAuthForCleanup()
    }

    private fun observeAuthForCleanup() {
        viewModelScope.launch {
            authViewModel.isAuthenticated.collect { isAuthenticated ->
                if (!isAuthenticated) {
                    // Clear user data when logged out
                    userViewModel.clearData()
                    mapViewModel.clearData()
                }
            }
        }
    }

    private fun observeUserState() {
        viewModelScope.launch {
            userViewModel.currentUser.collect { user ->
                if (user != null && authViewModel.isAuthenticated.value) {
                    // User is authenticated and has data, start map features
                    mapViewModel.initializeForUser(user)
                }
            }
        }
    }

    fun logout() = authViewModel.logout()

    fun startDataRefresh() = mapViewModel.startPeriodicRefresh()

    fun stopDataRefresh() = mapViewModel.stopPeriodicRefresh()

    fun updateCurrentUser(
        user: com.test.testing.discord.models.User,
        onComplete: (com.test.testing.discord.models.Result<Unit>) -> Unit = {},
    ) = userViewModel.updateCurrentUser(user, onComplete)

    fun deleteUserData(onComplete: (com.test.testing.discord.models.Result<Unit>) -> Unit = {}) = userViewModel.deleteUserData(onComplete)

    override fun onCleared() {
        super.onCleared()
        // ViewModels are automatically cleaned up by the framework
    }
}
