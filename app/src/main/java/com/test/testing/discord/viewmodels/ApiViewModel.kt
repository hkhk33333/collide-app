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
 * Enhanced Coordinator ViewModel with improved resource management and error handling
 *
 * Features:
 * - Lifecycle-aware coroutine management
 * - Enhanced error recovery
 * - Resource cleanup
 * - Performance monitoring
 */
class ApiViewModel(
    application: Application,
) : AndroidViewModel(application),
    DomainEventSubscriber {
    // Enhanced resource management
    private val coroutineManager = LifecycleAwareCoroutineManager()
    private val resourceManager = ViewModelResourceManager()
    private val performanceMonitor = PerformanceMonitor()

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
        // Cleanup resources in proper order
        coroutineManager.setActive(false)
        resourceManager.cleanup()
        eventBus.unsubscribe(this)
        mapViewModel.stopPeriodicRefresh()

        // Log performance metrics if in debug
        if (com.test.testing.discord.config.AppConfig.Features.ENABLE_PERFORMANCE_MONITORING) {
            logPerformanceMetrics()
        }
    }

    private fun logPerformanceMetrics() {
        println("=== Performance Metrics ===")
        performanceMonitor.operationMetrics.forEach { (operation, durations) ->
            val avgDuration = performanceMonitor.getAverageDuration(operation)
            val count = performanceMonitor.getOperationCount(operation)
            println("$operation: $count calls, avg ${avgDuration?.toLong()}ms")
        }
    }

    /**
     * Enhanced loadInitialData with resilience
     */
    fun loadInitialDataWithTracking() {
        coroutineManager.launch {
            val startTime = System.currentTimeMillis()

            try {
                // Both ViewModels handle their own initialization
                // The coordination happens through the event system

                val duration = System.currentTimeMillis() - startTime
                performanceMonitor.recordOperation("loadInitialData", duration)
            } catch (e: Exception) {
                // Handle initialization errors gracefully
                println("Error during initial data load: ${e.message}")
            }
        }
    }

    /**
     * Enhanced manual refresh with resilience and performance tracking
     */
    fun manualRefreshWithTracking() {
        coroutineManager.launch {
            val startTime = System.currentTimeMillis()

            try {
                mapViewModel.refreshUsers()

                val duration = System.currentTimeMillis() - startTime
                performanceMonitor.recordOperation("manualRefresh", duration)
            } catch (e: Exception) {
                println("Error during manual refresh: ${e.message}")
            }
        }
    }
}
