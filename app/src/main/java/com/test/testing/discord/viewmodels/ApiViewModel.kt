package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.models.DomainEvent
import com.test.testing.discord.models.DomainEventSubscriber
import com.test.testing.discord.models.SimpleEventBus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Coordinator ViewModel that manages the overall application state and coordinates
 * between specialized ViewModels (MapViewModel, UserViewModel).
 *
 * This follows the Single Responsibility Principle by delegating specific concerns
 * to focused ViewModels while maintaining overall application coordination.
 */
class ApiViewModel(
    application: Application,
) : AndroidViewModel(application),
    DomainEventSubscriber {
    // Specialized ViewModels for different concerns
    val mapViewModel = MapViewModel(application)
    val userViewModel = UserViewModel(application)

    private val eventBus = SimpleEventBus()

    // Combined state flows for convenience
    val currentUser = userViewModel.currentUser
    val users =
        mapViewModel.uiState.map { state ->
            when (state) {
                is com.test.testing.discord.ui.map.MapScreenUiState.Success -> state.users
                else -> emptyList()
            }
        }
    val guilds = userViewModel.guilds
    val uiState = mapViewModel.uiState
    val isLoading =
        userViewModel.isLoading.combine(mapViewModel.uiState) { userLoading, mapState ->
            userLoading || mapState is com.test.testing.discord.ui.map.MapScreenUiState.Loading
        }

    init {
        eventBus.subscribe(this)

        // Start periodic refresh when user data is loaded
        viewModelScope.launch {
            userViewModel.currentUser.collect { user ->
                if (user != null) {
                    mapViewModel.startPeriodicRefresh()
                } else {
                    mapViewModel.stopPeriodicRefresh()
                }
            }
        }
    }

    /**
     * Loads initial data by coordinating both specialized ViewModels
     */
    fun loadInitialData() {
        // Both ViewModels handle their own initialization
        // The coordination happens through the event system
    }

    /**
     * Manual refresh that delegates to the MapViewModel
     */
    fun manualRefresh() = mapViewModel.refreshUsers()

    /**
     * Updates current user through the UserViewModel
     */
    fun updateCurrentUser(
        user: com.test.testing.discord.models.User,
        onComplete: (com.test.testing.discord.models.Result<Unit>) -> Unit = {},
    ) = userViewModel.updateCurrentUser(user, onComplete)

    /**
     * Deletes user data through the UserViewModel
     */
    fun deleteUserData(onComplete: (com.test.testing.discord.models.Result<Unit>) -> Unit = {}) = userViewModel.deleteUserData(onComplete)

    /**
     * Logs out user through the UserViewModel
     */
    fun logout(onComplete: () -> Unit = {}) = userViewModel.logout(onComplete)

    /**
     * Clears all data by coordinating both ViewModels
     */
    fun clearData() {
        userViewModel.logout()
        mapViewModel.stopPeriodicRefresh()
    }

    /**
     * Starts data refresh (delegates to MapViewModel)
     */
    fun startDataRefresh() = mapViewModel.startPeriodicRefresh()

    /**
     * Stops data refresh (delegates to MapViewModel)
     */
    fun stopDataRefresh() = mapViewModel.stopPeriodicRefresh()

    override fun onEvent(event: DomainEvent) {
        when (event) {
            is DomainEvent.UserLoggedOut -> {
                mapViewModel.stopPeriodicRefresh()
            }
            is DomainEvent.DataCleared -> {
                mapViewModel.stopPeriodicRefresh()
            }
            else -> {
                // Events are handled by individual ViewModels
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventBus.unsubscribe(this)
        mapViewModel.stopPeriodicRefresh()
    }
}
