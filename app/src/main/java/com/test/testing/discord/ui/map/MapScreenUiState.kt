package com.test.testing.discord.ui.map

import com.test.testing.discord.models.User

data class MapScreenUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: String? = null,
)
