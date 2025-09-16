package com.test.testing.discord.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.test.testing.discord.ui.login.AuthIntent
import com.test.testing.discord.ui.login.AuthScreenUiState
import com.test.testing.discord.ui.login.LoginScreen
import com.test.testing.discord.ui.main.MainScreen
import com.test.testing.discord.viewmodels.AuthViewModel
import com.test.testing.discord.viewmodels.MainScreenViewModel

@Composable
fun DiscordApp() {
    ErrorBoundary {
        NetworkAware {
            val authViewModel: AuthViewModel = viewModel()
            val mainScreenViewModel: MainScreenViewModel = viewModel()
            val uiState by authViewModel.uiState.collectAsState()

            when (uiState) {
                is AuthScreenUiState.Authenticated -> {
                    MainScreen(locationManager = mainScreenViewModel.locationManager)
                }

                is AuthScreenUiState.Unauthenticated -> {
                    LoginScreen(onLoginClick = { activityContext ->
                        authViewModel.processIntent(AuthIntent.Login(activityContext))
                    })
                }

                is AuthScreenUiState.Loading -> {
                    // Show loading state
                    LoadingScreen()
                }

                is AuthScreenUiState.Error -> {
                    val errorState = uiState as AuthScreenUiState.Error
                    ErrorScreen(
                        message = errorState.message,
                        canRetry = errorState.canRetry,
                        onRetry = { authViewModel.processIntent(AuthIntent.ClearError) },
                    )
                }

                is AuthScreenUiState.Initial -> {
                    // Initial loading state
                    LoadingScreen()
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    // Simple loading screen - could be enhanced later
    Text("Loading...")
}

@Composable
private fun ErrorScreen(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    Column {
        Text("Error: $message")
        if (canRetry) {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
