package com.test.testing.discord.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.ui.login.LoginScreen
import com.test.testing.discord.ui.main.MainScreen

@Composable
fun DiscordApp() {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val isAuthenticated by authManager.isAuthenticated.collectAsState()

    if (isAuthenticated) {
        MainScreen()
    } else {
        LoginScreen(onLoginClick = { activityContext ->
            authManager.login(activityContext)
        })
    }
}
