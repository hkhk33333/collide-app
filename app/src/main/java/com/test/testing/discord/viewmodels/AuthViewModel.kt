package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.models.DomainEvent
import com.test.testing.discord.models.DomainEventBus
import com.test.testing.discord.models.DomainEventSubscriber
import com.test.testing.discord.ui.login.AuthEffect
import com.test.testing.discord.ui.login.AuthIntent
import com.test.testing.discord.ui.login.AuthScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Authentication ViewModel
 *
 * Responsibilities:
 * - Manage authentication state
 * - Handle login/logout operations
 * - Provide authentication status to other ViewModels
 */
@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        application: Application,
        private val savedStateHandle: SavedStateHandle,
        private val authManager: AuthManager,
        private val eventBus: DomainEventBus,
    ) : AndroidViewModel(application),
        DomainEventSubscriber {
        companion object {
            private const val KEY_AUTH_STATE = "auth_screen_state"
        }

        private val _state =
            MutableStateFlow(
                savedStateHandle.get<AuthScreenUiState>(KEY_AUTH_STATE) ?: AuthScreenUiState.Initial,
            )
        val state: StateFlow<AuthScreenUiState> = _state.asStateFlow()

        private val _effects = Channel<AuthEffect>()
        val effects = _effects.receiveAsFlow()

        // Legacy uiState for backward compatibility
        val uiState: StateFlow<AuthScreenUiState> = _state.asStateFlow()

        // Computed properties for backward compatibility
        val isAuthenticated: StateFlow<Boolean> =
            uiState
                .map { state: AuthScreenUiState -> state.isAuthenticated }
                .stateIn(viewModelScope, SharingStarted.Lazily, authManager.isAuthenticated.value)

        val isLoading: StateFlow<Boolean> =
            uiState
                .map { state: AuthScreenUiState -> state.isLoading }
                .stateIn(viewModelScope, SharingStarted.Lazily, false)

        init {
            eventBus.subscribe(this)
            // Auto-save state whenever it changes
            viewModelScope.launch {
                state.collect { currentState ->
                    savedStateHandle[KEY_AUTH_STATE] = currentState
                }
            }
            initializeAuthState()
        }

        private fun initializeAuthState() {
            val initialState =
                when {
                    authManager.isAuthenticated.value -> AuthScreenUiState.Authenticated()
                    else -> AuthScreenUiState.Unauthenticated()
                }
            _state.value = initialState

            // Observe auth state changes
            viewModelScope.launch {
                authManager.isAuthenticated.collect { authenticated ->
                    val newState =
                        if (authenticated) {
                            AuthScreenUiState.Authenticated()
                        } else {
                            AuthScreenUiState.Unauthenticated()
                        }
                    _state.value = newState

                    // Send effects based on auth state changes
                    if (authenticated) {
                        sendEffect(AuthEffect.LoginSuccess)
                    } else {
                        sendEffect(AuthEffect.LogoutSuccess)
                        eventBus.publish(DomainEvent.UserLoggedOut)
                    }
                }
            }
        }

        // MVI pattern implementation
        fun processIntent(intent: AuthIntent) {
            when (intent) {
                is AuthIntent.Login -> {
                    val newState = reduce(_state.value, intent)
                    _state.value = newState
                    performLogin(intent.context)
                }

                is AuthIntent.Logout -> {
                    val newState = reduce(_state.value, intent)
                    _state.value = newState
                    performLogout()
                }

                is AuthIntent.ClearError -> {
                    val newState = reduce(_state.value, intent)
                    _state.value = newState
                }

                is AuthIntent.CheckAuthStatus -> {
                    // Just update the state based on current auth status
                    initializeAuthState()
                }
            }
        }

        private fun reduce(
            state: AuthScreenUiState,
            intent: AuthIntent,
        ): AuthScreenUiState =
            when (intent) {
                is AuthIntent.Login -> {
                    AuthScreenUiState.Loading()
                }

                is AuthIntent.Logout -> {
                    AuthScreenUiState.Loading()
                }

                is AuthIntent.ClearError -> {
                    if (state is AuthScreenUiState.Error) {
                        AuthScreenUiState.Unauthenticated()
                    } else {
                        state
                    }
                }

                is AuthIntent.CheckAuthStatus -> {
                    state
                }
            }

        private fun performLogin(context: android.content.Context) {
            try {
                authManager.login(context)
                // Auth state change will be observed and update the state accordingly
            } catch (e: Exception) {
                _state.value =
                    AuthScreenUiState.Error(
                        message = "Failed to start login: ${e.message}",
                        canRetry = true,
                    )
            }
        }

        private fun performLogout() {
            authManager.logout {
                // Auth state change will be observed and update the state accordingly
                // Clear any error state
                val currentState = _state.value
                when (currentState) {
                    is AuthScreenUiState.Error -> {
                        _state.value = AuthScreenUiState.Unauthenticated()
                    }

                    else -> {
                        // State will be updated by initializeAuthState when authManager.isAuthenticated changes
                    }
                }
            }
        }

        // Legacy methods for backward compatibility
        fun onEvent(event: com.test.testing.discord.ui.UiEvent) {
            when (event) {
                is com.test.testing.discord.ui.UiEvent.Login -> {
                    // This method needs context, so we can't use it directly
                    // The UI should use processIntent instead
                }

                is com.test.testing.discord.ui.UiEvent.Logout -> {
                    processIntent(AuthIntent.Logout)
                }

                else -> {
                    // Handle other events if needed
                }
            }
        }

        fun login() {
            // Legacy method - context should be passed via intent
            // This is kept for backward compatibility but should be replaced
        }

        fun logout(onComplete: (() -> Unit)? = null) {
            processIntent(AuthIntent.Logout)
            // Call onComplete after logout completes
            viewModelScope.launch {
                effects.collect { effect ->
                    if (effect is AuthEffect.LogoutSuccess) {
                        onComplete?.invoke()
                    }
                }
            }
        }

        fun clearError() {
            processIntent(AuthIntent.ClearError)
        }

        private fun sendEffect(effect: AuthEffect) {
            viewModelScope.launch {
                _effects.send(effect)
            }
        }

        // Handle domain events
        override fun onEvent(event: DomainEvent) {
            when (event) {
                is DomainEvent.UserLoggedOut -> {
                    _state.value = AuthScreenUiState.Unauthenticated()
                }

                is DomainEvent.DataCleared -> {
                    _state.value = AuthScreenUiState.Unauthenticated()
                }

                else -> {
                    // Handle other events if needed
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            eventBus.unsubscribe(this)
            // AuthManager is a singleton, no cleanup needed
        }
    }
