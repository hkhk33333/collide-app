package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.models.DomainEvent
import com.test.testing.discord.models.DomainEventBus
import com.test.testing.discord.models.DomainEventSubscriber
import com.test.testing.discord.ui.login.AuthScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
        private val authManager: AuthManager,
        private val eventBus: DomainEventBus,
    ) : AndroidViewModel(application),
        DomainEventSubscriber {
        private val _uiState =
            MutableStateFlow<AuthScreenUiState>(
                if (authManager.isAuthenticated.value) {
                    AuthScreenUiState.Authenticated()
                } else {
                    AuthScreenUiState.Unauthenticated()
                },
            )
        val uiState: StateFlow<AuthScreenUiState> = _uiState.asStateFlow()

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
            observeAuthState()
        }

        private fun observeAuthState() {
            viewModelScope.launch {
                authManager.isAuthenticated.collect { authenticated ->
                    val newState =
                        if (authenticated) {
                            AuthScreenUiState.Authenticated()
                        } else {
                            AuthScreenUiState.Unauthenticated()
                        }
                    _uiState.value = newState
                    if (!authenticated) {
                        eventBus.publish(DomainEvent.UserLoggedOut)
                    }
                }
            }
        }

        // Handle UI events
        fun onEvent(event: com.test.testing.discord.ui.UiEvent) {
            when (event) {
                is com.test.testing.discord.ui.UiEvent.Login -> login()
                is com.test.testing.discord.ui.UiEvent.Logout -> logout()
                else -> {
                    // Handle other events if needed
                }

            }
        }

        fun login() {
            val currentState = _uiState.value
            when (currentState) {
                is AuthScreenUiState.Authenticated -> {
                    _uiState.value = currentState.copy(isLoading = true)
                }

                is AuthScreenUiState.Unauthenticated -> {
                    _uiState.value = currentState.copy(isLoading = true)
                }

                else -> {
                    _uiState.value = AuthScreenUiState.Loading()
                }
            }
            // AuthManager handles the actual login flow - context is passed from UI
            // The auth state change will be observed and update the UiState accordingly
        }

        fun logout(onComplete: (() -> Unit)? = null) {
            val currentState = _uiState.value
            when (currentState) {
                is AuthScreenUiState.Authenticated -> {
                    _uiState.value = currentState.copy(isLoading = true)
                }
                is AuthScreenUiState.Unauthenticated -> {
                    _uiState.value = currentState.copy(isLoading = true)
                }
                else -> {
                    _uiState.value = AuthScreenUiState.Loading()
                }
            }

            authManager.logout {
                // The auth state change will be observed and update the UiState accordingly
                // Clear any error state
                val currentUiState = _uiState.value
                when (currentUiState) {
                    is AuthScreenUiState.Error -> {
                        _uiState.value = AuthScreenUiState.Unauthenticated()
                    }

                    else -> {
                        // UiState will be updated by observeAuthState when authManager.isAuthenticated changes
                    }
                }
                onComplete?.invoke()
            }
        }

        fun clearError() {
            val currentState = _uiState.value
            if (currentState is AuthScreenUiState.Error) {
                _uiState.value = AuthScreenUiState.Unauthenticated()
            }
        }

        // Handle domain events
        override fun onEvent(event: DomainEvent) {
            when (event) {
                is DomainEvent.UserLoggedOut -> {
                    _uiState.value = AuthScreenUiState.Unauthenticated()
                }

                is DomainEvent.DataCleared -> {
                    _uiState.value = AuthScreenUiState.Unauthenticated()
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
