package com.test.testing.discord.ui.map

import com.test.testing.discord.models.User

sealed interface MapScreenUiState {
    object Loading : MapScreenUiState

    data class Success(
        val users: List<User> = emptyList(),
        val isRefreshing: Boolean = false,
        val lastUpdated: Long = System.currentTimeMillis(),
    ) : MapScreenUiState

    sealed class Error : MapScreenUiState {
        data class NetworkError(
            val message: String,
            val canRetry: Boolean = true,
        ) : Error()

        data class AuthenticationError(
            val message: String = "Authentication failed. Please log in again.",
        ) : Error()

        data class ServerError(
            val message: String,
            val code: Int? = null,
        ) : Error()

        data class UnknownError(
            val message: String,
            val exception: Exception? = null,
        ) : Error()
    }
}
