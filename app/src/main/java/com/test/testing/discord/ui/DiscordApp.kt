package com.test.testing.discord.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.ui.login.LoginScreen
import com.test.testing.discord.ui.main.MainScreen
import com.test.testing.discord.viewmodels.ApiViewModel

@Composable
fun DiscordApp() {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val isAuthenticated by authManager.isAuthenticated.collectAsState()
    val apiViewModel: ApiViewModel = viewModel()

    if (isAuthenticated) {
        LaunchedEffect(Unit) {
            apiViewModel.loadInitialData()
        }
        MainScreen(apiViewModel = apiViewModel)
    } else {
        // CHANGE IS HERE: Update the call to pass the context to authManager.login
        LoginScreen(onLoginClick = { activityContext ->
            authManager.login(activityContext)
        })
    }
}
