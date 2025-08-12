package com.test.testing.auth

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AuthNavigation(
    authViewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    content: @Composable () -> Unit,
) {
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN) }

    // Observe authentication state
    LaunchedEffect(authViewModel.authState) {
        if (authViewModel.authState == AuthState.AUTHENTICATED) {
            onAuthenticated()
        }
    }

    when (authViewModel.authState) {
        AuthState.AUTHENTICATED -> {
            content()
        }
        else -> {
            when (currentScreen) {
                AuthScreen.LOGIN -> {
                    LoginScreen(
                        authViewModel = authViewModel,
                        onNavigateToRegister = { currentScreen = AuthScreen.REGISTER },
                    )
                }
                AuthScreen.REGISTER -> {
                    RegisterScreen(
                        authViewModel = authViewModel,
                        onNavigateToLogin = { currentScreen = AuthScreen.LOGIN },
                    )
                }
            }
        }
    }
}

enum class AuthScreen {
    LOGIN,
    REGISTER,
}
