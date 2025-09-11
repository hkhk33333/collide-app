package com.test.testing.discord.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.ui.login.LoginScreen
import com.test.testing.discord.ui.main.MainScreen
import com.test.testing.discord.viewmodels.AppViewModel

@Composable
fun DiscordApp() {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val isAuthenticated by authManager.isAuthenticated.collectAsState()
    val appViewModel: AppViewModel = viewModel()

    if (isAuthenticated) {
        LaunchedEffect(Unit) {
            // AppViewModel handles initialization automatically
        }
        MainScreen(appViewModel = appViewModel)
    } else {
        LoginScreen(onLoginClick = { activityContext ->
            authManager.login(activityContext)
        })
    }
}
