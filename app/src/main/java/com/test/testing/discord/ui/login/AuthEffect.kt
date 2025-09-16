package com.test.testing.discord.ui.login

/**
 * Effects for authentication operations following MVI pattern
 */
sealed class AuthEffect {
    data class ShowError(
        val message: String,
    ) : AuthEffect()

    object LoginSuccess : AuthEffect()

    object LogoutSuccess : AuthEffect()

    object NavigateToMain : AuthEffect()
}
