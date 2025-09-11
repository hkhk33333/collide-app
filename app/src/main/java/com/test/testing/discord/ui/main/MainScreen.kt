package com.test.testing.discord.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.ui.map.MapScreen
import com.test.testing.discord.ui.settings.SettingsScreen
import com.test.testing.discord.viewmodels.AppViewModel

sealed class Screen(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
) {
    object Map : Screen("map", "Map", { Icon(Icons.Filled.Map, contentDescription = "Map") })

    object Settings : Screen("settings", "Settings", { Icon(Icons.Filled.Settings, contentDescription = "Settings") })
}

@Composable
fun MainScreen(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val locationManager = remember { LocationManager.getInstance(context) }

    // Use a lifecycle effect to start/stop the timer when the MainScreen is shown/hidden
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        appViewModel.startDataRefresh()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        // AppViewModel handles this automatically
    }

    PermissionHandler(locationManager)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(Screen.Map, Screen.Settings)

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { screen.icon() },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Map.route, Modifier.padding(innerPadding)) {
            composable(Screen.Map.route) { MapScreen(appViewModel.mapViewModel, locationManager) }
            composable(Screen.Settings.route) { SettingsScreen(appViewModel, locationManager) }
        }
    }
}

@Composable
fun PermissionHandler(locationManager: LocationManager) {
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                locationManager.startLocationUpdates()
            }
        }

    LaunchedEffect(Unit) {
        val permissionsToRequest =
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
}
