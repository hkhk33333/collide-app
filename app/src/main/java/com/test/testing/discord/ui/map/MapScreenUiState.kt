package com.test.testing.discord.ui.map

import com.test.testing.discord.models.User
import com.test.testing.discord.ui.UiAction
import com.test.testing.discord.ui.UiState

/**
 * Enhanced UI state for the map screen with better error handling and actions
 */
sealed interface MapScreenUiState {
    val users: List<User> get() = emptyList()
    val isRefreshing: Boolean get() = false
    val lastUpdated: Long get() = System.currentTimeMillis()

    object Loading : MapScreenUiState

    data class Success(
        override val users: List<User> = emptyList(),
        override val isRefreshing: Boolean = false,
        override val lastUpdated: Long = System.currentTimeMillis(),
        val selectedUserId: String? = null,
        val isLocationEnabled: Boolean = false,
    ) : MapScreenUiState

    data class Error(
        val message: String,
        val errorType: com.test.testing.discord.models.ErrorType = com.test.testing.discord.models.ErrorType.UNKNOWN,
        val canRetry: Boolean = false,
        val actions: List<UiAction> = emptyList(),
    ) : MapScreenUiState {
        val shouldShowRetryButton: Boolean = canRetry && actions.contains(UiAction.Retry)
    }

    companion object {
        fun fromUiState(
            uiState: UiState<List<User>>,
            isRefreshing: Boolean = false,
        ): MapScreenUiState =
            when (uiState) {
                is UiState.Loading -> Loading
                is UiState.Success ->
                    Success(
                        users = uiState.data,
                        isRefreshing = isRefreshing,
                    )
                is UiState.Error ->
                    Error(
                        message = uiState.message,
                        canRetry = uiState.canRetry,
                        actions = if (uiState.canRetry) listOf(UiAction.Retry) else emptyList(),
                    )
            }
    }
}

// Legacy type aliases for backward compatibility
typealias NetworkError = MapScreenUiState.Error
typealias AuthenticationError = MapScreenUiState.Error
typealias ServerError = MapScreenUiState.Error
typealias UnknownError = MapScreenUiState.Error
