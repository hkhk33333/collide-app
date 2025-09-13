package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.test.testing.BuildConfig
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.cache.CacheManager
import com.test.testing.discord.domain.usecase.GetUsersUseCase
import com.test.testing.discord.models.*
import com.test.testing.discord.ui.map.MapEffect
import com.test.testing.discord.ui.map.MapIntent
import com.test.testing.discord.ui.map.MapScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        application: Application,
        private val savedStateHandle: SavedStateHandle,
        private val getUsersUseCase: GetUsersUseCase,
        private val authManager: AuthManager,
        private val cacheManager: CacheManager,
        private val eventBus: DomainEventBus,
    ) : AndroidViewModel(application),
        DomainEventSubscriber {
        companion object {
            private const val KEY_MAP_STATE = "map_screen_state"
        }

        private val _state =
            MutableStateFlow(
                savedStateHandle.get<MapScreenState>(KEY_MAP_STATE) ?: MapScreenState.Loading,
            )
        val state: StateFlow<MapScreenState> = _state.asStateFlow()

        private val _effects = Channel<MapEffect>()
        val effects = _effects.receiveAsFlow()

        val users: StateFlow<List<com.test.testing.discord.domain.models.DomainUser>> =
            state
                .map { state ->
                    when (state) {
                        is MapScreenState.Content -> state.users
                        else -> emptyList()
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // Unfiltered users list for settings - includes blocked users so they can be unblocked
        val allUsers: StateFlow<List<com.test.testing.discord.domain.models.DomainUser>> =
            state
                .map { state ->
                    when (state) {
                        is MapScreenState.Content -> state.allUsers
                        else -> emptyList()
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        private var refreshJob: Job? = null
        private val refreshInterval = 30000L // 30 seconds

        private val token: String?
            get() =
                authManager.token.value
                    ?.let { "Bearer $it" }

        // Store current user's privacy settings for synchronous access
        private var currentUserPrivacySettings: com.test.testing.discord.domain.models.UserPrivacySettings? = null

        /**
         * Updates stored privacy settings when user data changes
         */
        private fun updatePrivacySettings(user: com.test.testing.discord.domain.models.DomainUser?) {
            currentUserPrivacySettings = user?.privacySettings
        }

        /**
         * Filters users based on current user's privacy settings
         */
        private fun filterUsersByPrivacySettings(
            users: List<com.test.testing.discord.domain.models.DomainUser>,
        ): List<com.test.testing.discord.domain.models.DomainUser> {
            val privacySettings = currentUserPrivacySettings
            if (privacySettings == null) {
                // If no privacy settings available, return all users (fallback)
                return users
            }

            val enabledGuilds = privacySettings.enabledGuilds.map { it.value }.toSet()
            val blockedUserIds = privacySettings.blockedUsers.map { it.value }.toSet()

            return users.filter { user ->
                // Don't filter out the current user (we don't have access to current user ID here)
                // In a real implementation, we'd compare user IDs

                // Filter out blocked users
                if (blockedUserIds.contains(user.id.value)) {
                    return@filter false
                }

                // For now, show all users that aren't blocked
                // In a real app, you'd check if the user is in enabled guilds
                return@filter true
            }
        }

        private val exceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                _state.value = mapExceptionToUiState(throwable as? Exception ?: Exception("Unknown error"))
            }

        init {
            eventBus.subscribe(this)
            // Save state whenever it changes
            viewModelScope.launch {
                state.collect { currentState ->
                    savedStateHandle[KEY_MAP_STATE] = currentState
                }
            }
            // Initialize privacy settings from cache
            viewModelScope.launch {
                try {
                    val cachedUser = cacheManager.get<User>(CacheManager.CacheKey.CURRENT_USER)
                    if (cachedUser != null) {
                        // Convert User to DomainUser for updatePrivacySettings
                        // In a real implementation, we'd use a mapper here
                        // For now, create a basic DomainUser from cached data
                        val domainUser =
                            com.test.testing.discord.domain.models.DomainUser(
                                id =
                                    com.test.testing.discord.domain.models
                                        .UserId(cachedUser.id),
                                username =
                                    com.test.testing.discord.domain.models
                                        .Username(cachedUser.duser.username),
                                avatarUrl = cachedUser.duser.avatar ?: "",
                                location = null, // Not available in cache
                                isOnline = true,
                                lastSeen = java.time.Instant.now(),
                                privacySettings =
                                    com.test.testing.discord.domain.models.UserPrivacySettings(
                                        enabledGuilds =
                                            cachedUser.privacy.enabledGuilds
                                                .map {
                                                    com.test.testing.discord.domain.models.GuildId(
                                                        it,
                                                    )
                                                }.toSet(),
                                        blockedUsers =
                                            cachedUser.privacy.blockedUsers
                                                .map {
                                                    com.test.testing.discord.domain.models.UserId(
                                                        it,
                                                    )
                                                }.toSet(),
                                        locationSharingEnabled = true,
                                        nearbyNotificationsEnabled = cachedUser.receiveNearbyNotifications ?: true,
                                    ),
                            )
                        updatePrivacySettings(domainUser)
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.w("MapViewModel", "Failed to load privacy settings from cache", e)
                    }
                    // Ignore cache errors during initialization
                }
            }
            loadUsers()
        }

        // Process user intents following MVI pattern
        fun processIntent(intent: MapIntent) {
            val currentState = _state.value
            val newState = reduce(currentState, intent)
            _state.value = newState

            // Handle side effects after state update
            when (intent) {
                is MapIntent.RefreshUsers -> {
                    refreshUsers()
                }

                is MapIntent.LoadUsers -> {
                    loadUsers()
                }

                // Other intents don't need side effects
                else -> {}
            }
        }

        // Reducer function for state transformations
        private fun reduce(
            state: MapScreenState,
            intent: MapIntent,
        ): MapScreenState =
            when (intent) {
                is MapIntent.LoadUsers -> {
                    if (token == null) {
                        MapScreenState.Error(
                            message = "Authentication required. Please log in again.",
                            errorType = ErrorType.AUTHENTICATION,
                        )
                    } else {
                        MapScreenState.Loading
                    }
                }

                is MapIntent.RefreshUsers -> {
                    when (state) {
                        is MapScreenState.Content -> state.copy()
                        is MapScreenState.Error -> state.copy()
                        else -> state
                    }
                }

                is MapIntent.UserSelected -> {
                    if (state is MapScreenState.Content) {
                        state.copy(selectedUserId = intent.userId)
                    } else {
                        state
                    }
                }

                is MapIntent.LocationPermissionChanged -> {
                    if (state is MapScreenState.Content) {
                        state.copy(isLocationEnabled = intent.granted)
                    } else {
                        state
                    }
                }
            }

        // Legacy method for backward compatibility during migration
        fun onEvent(event: com.test.testing.discord.ui.UiEvent) {
            when (event) {
                is com.test.testing.discord.ui.UiEvent.RefreshUsers -> {
                    processIntent(MapIntent.RefreshUsers)
                }

                is com.test.testing.discord.ui.UiEvent.LoadUsers -> {
                    processIntent(MapIntent.LoadUsers)
                }

                else -> {
                    // Handle other events if needed
                }
            }
        }

        private fun loadUsers() {
            if (token == null) {
                _state.value =
                    MapScreenState.Error(
                        message = "Authentication required. Please log in again.",
                        errorType = ErrorType.AUTHENTICATION,
                    )
                return
            }

            // Don't set loading state here - it's handled by the reducer when LoadUsers intent is processed
            viewModelScope.launch(exceptionHandler) {
                getUsersUseCase(token!!, forceRefresh = false).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val filteredUsers = filterUsersByPrivacySettings(result.data)
                            _state.value =
                                MapScreenState.Content(
                                    users = filteredUsers,
                                    allUsers = result.data, // Store unfiltered list for settings
                                )
                            eventBus.publish(DomainEvent.DataRefreshCompleted(true))
                        }

                        is Result.Error -> {
                            _state.value = mapExceptionToUiState(result.exception)
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
            val currentState = _state.value

            if (BuildConfig.DEBUG) {
                android.util.Log.d("MapViewModel", "refreshUsers called, current state: $currentState")
            }

            return when {
                currentState is MapScreenState.Loading -> {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MapViewModel", "Refresh prevented - already loading")
                    }
                    false
                }

                currentState is MapScreenState.Content && currentState.isRefreshing -> {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MapViewModel", "Refresh prevented - already refreshing")
                    }
                    false
                }

                token == null -> {
                    handleMissingToken()
                    false
                }

                else -> {
                    true
                }
            }
        }

        private fun handleMissingToken() {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("MapViewModel", "No token available")
            }
            _state.value =
                MapScreenState.Error(
                    message = "Authentication required. Please log in again.",
                    errorType = ErrorType.AUTHENTICATION,
                )
        }

        private fun setRefreshingState() {
            val currentState = _state.value
            if (currentState is MapScreenState.Content) {
                _state.value = currentState.copy(isRefreshing = true)
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

        private fun handleRefreshResult(result: Result<List<com.test.testing.discord.domain.models.DomainUser>>) {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("MapViewModel", "Received result: $result")
            }

            _state.value =
                when (result) {
                    is Result.Success -> {
                        val filteredUsers = filterUsersByPrivacySettings(result.data)
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("MapViewModel", "Set state to Success with ${filteredUsers.size} filtered users")
                        }
                        MapScreenState.Content(
                            users = filteredUsers,
                            allUsers = result.data, // Store unfiltered list for settings
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

        private fun mapExceptionToUiState(exception: Exception): MapScreenState =
            when {
                exception.message?.contains("401") == true ||
                    exception.message?.contains("403") == true -> {
                    MapScreenState.Error(
                        message = "Authentication failed. Please log in again.",
                        errorType = ErrorType.AUTHENTICATION,
                    )
                }

                exception.message?.contains("5") == true -> {
                    MapScreenState.Error(
                        message = "Server error occurred. Please try again later.",
                        errorType = ErrorType.SERVER,
                    )
                }

                else -> {
                    MapScreenState.Error(
                        message = exception.message ?: "Network error occurred",
                        errorType = ErrorType.NETWORK,
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
            _state.value =
                MapScreenState.Content(
                    users = emptyList(),
                )
        }

        override fun onEvent(event: DomainEvent) {
            when (event) {
                is DomainEvent.UserLoggedOut -> {
                    stopPeriodicRefresh()
                    _state.value =
                        MapScreenState.Error(
                            message = "Authentication required. Please log in again.",
                            errorType = ErrorType.AUTHENTICATION,
                        )
                }

                is DomainEvent.DataCleared -> {
                    _state.value =
                        MapScreenState.Content(
                            users = emptyList(),
                        )
                }

                is DomainEvent.UserDataUpdated -> {
                    // Update stored privacy settings and refresh the user list
                    // to reflect new visibility settings
                    updatePrivacySettings(event.user)
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MapViewModel", "User data updated, refreshing users due to privacy changes")
                    }
                    refreshUsers()
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
