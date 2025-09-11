package com.test.testing.discord.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.models.DomainEvent
import com.test.testing.discord.models.SimpleEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Authentication ViewModel
 *
 * Responsibilities:
 * - Manage authentication state
 * - Handle login/logout operations
 * - Provide authentication status to other ViewModels
 */
class AuthViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val authManager = AuthManager.getInstance(application)
    private val eventBus = SimpleEventBus()

    private val _isAuthenticated = MutableStateFlow(authManager.isAuthenticated.value)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.isAuthenticated.collect { authenticated ->
                _isAuthenticated.value = authenticated
                if (!authenticated) {
                    eventBus.publish(DomainEvent.UserLoggedOut)
                }
            }
        }
    }

    fun login() {
        _isLoading.value = true
        _error.value = null
        // AuthManager handles the actual login flow - context is passed from UI
        _isLoading.value = false
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        _isLoading.value = true
        authManager.logout {
            _isLoading.value = false
            _error.value = null
            onComplete?.invoke()
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // AuthManager is a singleton, no cleanup needed
    }
}
