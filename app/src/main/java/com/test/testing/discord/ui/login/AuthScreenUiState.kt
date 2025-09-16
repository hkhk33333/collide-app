package com.test.testing.discord.ui.login

import android.os.Parcelable
import androidx.annotation.Keep
import com.test.testing.discord.ui.UiAction
import kotlinx.parcelize.Parcelize

/**
 * Enhanced UI state for the authentication screen with better error handling and actions
 */
@Keep
sealed class AuthScreenUiState : Parcelable {
    abstract val isAuthenticated: Boolean
    abstract val isLoading: Boolean
    abstract val lastUpdated: Long

    @Parcelize
    object Initial : AuthScreenUiState() {
        override val isAuthenticated: Boolean = false
        override val isLoading: Boolean = false
        override val lastUpdated: Long = System.currentTimeMillis()
    }

    @Parcelize
    data class Authenticated(
        override val isAuthenticated: Boolean = true,
        override val isLoading: Boolean = false,
        override val lastUpdated: Long = System.currentTimeMillis(),
    ) : AuthScreenUiState()

    @Parcelize
    data class Unauthenticated(
        override val isAuthenticated: Boolean = false,
        override val isLoading: Boolean = false,
        override val lastUpdated: Long = System.currentTimeMillis(),
    ) : AuthScreenUiState()

    @Parcelize
    data class Loading(
        override val isAuthenticated: Boolean = false,
        override val isLoading: Boolean = true,
        override val lastUpdated: Long = System.currentTimeMillis(),
    ) : AuthScreenUiState()

    @Parcelize
    data class Error(
        val message: String,
        val errorType: com.test.testing.discord.models.ErrorType = com.test.testing.discord.models.ErrorType.UNKNOWN,
        val canRetry: Boolean = false,
        val actions: List<UiAction> = emptyList(),
        override val isAuthenticated: Boolean = false,
        override val isLoading: Boolean = false,
    ) : AuthScreenUiState() {
        override val lastUpdated: Long = System.currentTimeMillis()
        val shouldShowRetryButton: Boolean = canRetry && actions.contains(UiAction.Retry)
    }

    companion object {
        fun fromUiState(
            isAuthenticated: Boolean,
            isLoading: Boolean = false,
            error: String? = null,
        ): AuthScreenUiState =
            when {
                error != null -> {
                    Error(
                        message = error,
                        canRetry = true,
                        actions = listOf(UiAction.Retry),
                    )
                }

                isLoading -> {
                    Loading(isAuthenticated = isAuthenticated)
                }

                isAuthenticated -> {
                    Authenticated()
                }

                else -> {
                    Unauthenticated()
                }
            }
    }
}

// Legacy type aliases for backward compatibility
typealias AuthNetworkError = AuthScreenUiState.Error
typealias AuthAuthenticationError = AuthScreenUiState.Error
typealias AuthServerError = AuthScreenUiState.Error
typealias AuthUnknownError = AuthScreenUiState.Error
