package com.test.testing.discord.ui.settings

import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.User
import com.test.testing.discord.ui.UiAction
import com.test.testing.discord.ui.UiState

/**
 * Enhanced UI state for the user/settings screen with better error handling and actions
 */
sealed interface UserScreenUiState {
    val currentUser: User? get() = null
    val guilds: List<Guild> get() = emptyList()
    val isLoading: Boolean get() = false
    val lastUpdated: Long get() = System.currentTimeMillis()

    object Loading : UserScreenUiState

    data class Success(
        override val currentUser: User? = null,
        override val guilds: List<Guild> = emptyList(),
        override val isLoading: Boolean = false,
        override val lastUpdated: Long = System.currentTimeMillis(),
    ) : UserScreenUiState

    data class Error(
        val message: String,
        val errorType: com.test.testing.discord.models.ErrorType = com.test.testing.discord.models.ErrorType.UNKNOWN,
        val canRetry: Boolean = false,
        val actions: List<UiAction> = emptyList(),
        override val isLoading: Boolean = false,
    ) : UserScreenUiState {
        val shouldShowRetryButton: Boolean = canRetry && actions.contains(UiAction.Retry)
    }

    companion object {
        fun fromUiState(
            uiState: UiState<User?>,
            guilds: List<Guild> = emptyList(),
            isLoading: Boolean = false,
        ): UserScreenUiState =
            when (uiState) {
                is UiState.Loading -> Loading
                is UiState.Success ->
                    Success(
                        currentUser = uiState.data,
                        guilds = guilds,
                        isLoading = isLoading,
                    )
                is UiState.Error ->
                    Error(
                        message = uiState.message,
                        canRetry = uiState.canRetry,
                        actions = if (uiState.canRetry) listOf(UiAction.Retry) else emptyList(),
                        isLoading = isLoading,
                    )
            }
    }
}

// Legacy type aliases for backward compatibility
typealias UserNetworkError = UserScreenUiState.Error
typealias UserAuthenticationError = UserScreenUiState.Error
typealias UserServerError = UserScreenUiState.Error
typealias UserUnknownError = UserScreenUiState.Error
