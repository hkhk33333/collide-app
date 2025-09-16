package com.test.testing.discord.ui.login

import android.content.Context

/**
 * Intents for authentication operations following MVI pattern
 */
sealed class AuthIntent {
    data class Login(
        val context: Context,
    ) : AuthIntent()

    object Logout : AuthIntent()

    object ClearError : AuthIntent()

    object CheckAuthStatus : AuthIntent()
}
