package com.test.testing.discord.ui.map

import com.test.testing.discord.models.User

sealed interface MapScreenUiState {
    object Loading : MapScreenUiState

    data class Success(
        val users: List<User> = emptyList(),
        val isRefreshing: Boolean = false,
    ) : MapScreenUiState

    data class Error(
        val message: String,
    ) : MapScreenUiState
}
