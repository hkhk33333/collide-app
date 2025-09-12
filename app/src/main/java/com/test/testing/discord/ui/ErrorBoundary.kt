package com.test.testing.discord.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.test.testing.R
import com.test.testing.discord.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Global error boundary that catches unexpected crashes and provides recovery options
 */
@Composable
fun ErrorBoundary(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (hasError) {
        ErrorRecoveryScreen(
            errorMessage = errorMessage ?: "An unexpected error occurred",
            onRetry = {
                hasError = false
                errorMessage = null
            },
            modifier = modifier,
        )
    } else {
        content()
        // Note: Try-catch around composables is not allowed in Compose
        // Error boundaries should be implemented at the ViewModel level
    }
}

/**
 * Error recovery screen with network awareness and user-friendly messaging
 */
@Composable
private fun ErrorRecoveryScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()

    var isNetworkAvailable by remember { mutableStateOf(checkNetworkAvailability(context)) }
    var isRetrying by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Error icon
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Error",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error title
        Text(
            text = if (isNetworkAvailable) "Oops! Something went wrong" else "No Internet Connection",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        Text(
            text =
                if (isNetworkAvailable) {
                    errorMessage
                } else {
                    "Please check your internet connection and try again."
                },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isRetrying = true
                        // Sign out and restart
                        authViewModel.logout {
                            isRetrying = false
                        }
                    }
                },
                enabled = !isRetrying,
            ) {
                Text("Sign Out")
            }

            Button(
                onClick = {
                    scope.launch {
                        isRetrying = true
                        // Check network and retry
                        isNetworkAvailable = checkNetworkAvailability(context)
                        onRetry()
                        isRetrying = false
                    }
                },
                enabled = !isRetrying,
            ) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Try Again")
                }
            }
        }
    }
}

/**
 * Check if network is available
 */
private fun checkNetworkAvailability(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

/**
 * Network-aware composable wrapper
 */
@Composable
fun NetworkAware(
    offlineContent: @Composable () -> Unit = { DefaultOfflineContent() },
    onlineContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(checkNetworkAvailability(context)) }

    LaunchedEffect(Unit) {
        // Could add periodic network checks here
        isOnline = checkNetworkAvailability(context)
    }

    if (isOnline) {
        onlineContent()
    } else {
        offlineContent()
    }
}

/**
 * Default offline content
 */
@Composable
private fun DefaultOfflineContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Offline",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You're offline",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check your connection and try again",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
