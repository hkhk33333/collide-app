package com.test.testing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.test.testing.api.FirebaseLocationRepository
import com.test.testing.api.LocationModel
import com.test.testing.auth.AuthNavigation
import com.test.testing.auth.AuthViewModel
import com.test.testing.friends.AddFriendScreen
import com.test.testing.friends.FriendListScreen
import com.test.testing.friends.FriendRepository
import com.test.testing.ui.theme.TestingTheme

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var currentLocation by mutableStateOf<Location?>(null)
    private val locationRepository = FirebaseLocationRepository()
    private val friendRepository = FriendRepository()
    private var allLocations by mutableStateOf<Map<String, LocationModel>>(emptyMap())
    private var currentScreen by mutableStateOf<Screen>(Screen.MAP)
    private var friendToFocus by mutableStateOf<String?>(null)
    private var shouldCenterOnMyLocation by mutableStateOf(false)

    // We request foreground (while-in-use) location first, then handle background separately.
    // Rationale:
    // - Android 10 (API 29): background can be requested via the runtime permission dialog, but only AFTER foreground is granted.
    // - Android 11+ (API 30+): the system no longer grants background directly via the dialog; users must enable
    //   "Allow all the time" from app settings.
    // Docs:
    // - Request location permissions: https://developer.android.com/training/location/permissions
    // - Background location guidance: https://developer.android.com/training/location/permissions#background
    // - Request runtime permissions best practices: https://developer.android.com/training/permissions/requesting
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val locationPermissionGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (locationPermissionGranted) {
                startLocationUpdates()
                startBackgroundLocationService()

                // Request background location permission if not granted
                // Android 10 (API 29) only: request via launcher; Android 11+ requires Settings
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && !hasBackgroundLocationPermission()) {
                    requestBackgroundLocationPermission()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()

            TestingTheme {
                AuthNavigation(
                    authViewModel = authViewModel,
                    onAuthenticated = {
                        // Once authenticated, request location permissions
                        checkLocationPermissions()
                    },
                ) {
                    // Main app content when authenticated
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                        ) {
                            when (currentScreen) {
                                Screen.MAP -> {
                                    MapScreen(
                                        currentLocation = currentLocation,
                                        allLocations = allLocations,
                                        onMyLocationClick = {
                                            // Check permissions before starting location updates
                                            if (hasLocationPermissions()) {
                                                startLocationUpdates()
                                                friendToFocus = null
                                                shouldCenterOnMyLocation = true
                                            } else {
                                                checkLocationPermissions()
                                            }
                                        },
                                        onSignOut = { authViewModel.signOut() },
                                        onNavigateToFriends = { currentScreen = Screen.FRIENDS },
                                        friendToFocus = friendToFocus,
                                        shouldCenterOnMyLocation = shouldCenterOnMyLocation,
                                        onLocationCentered = { shouldCenterOnMyLocation = false },
                                    )
                                }

                                Screen.FRIENDS -> {
                                    FriendListScreen(
                                        friendRepository = friendRepository,
                                        onNavigateToAddFriend = {
                                            currentScreen = Screen.ADD_FRIEND
                                        },
                                        onNavigateBack = {
                                            friendToFocus = null
                                            currentScreen = Screen.MAP
                                        },
                                        onViewFriendLocation = { userId ->
                                            friendToFocus = userId
                                            currentScreen = Screen.MAP
                                        },
                                    )
                                }

                                Screen.ADD_FRIEND -> {
                                    AddFriendScreen(
                                        friendRepository = friendRepository,
                                        onNavigateBack = { currentScreen = Screen.FRIENDS },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasLocationPermissions(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Returns true if background location is granted. On Android 9 and below (API 28 and lower)
     * there is no separate background location permission, so we treat it as granted when
     * foreground is granted.
     */
    private fun hasBackgroundLocationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is granted
                startLocationUpdates()
                startBackgroundLocationService()
            }

            else -> {
                // Request basic location permissions first (background location must be requested separately)
                val permissions =
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )

                requestPermissionLauncher.launch(permissions)
            }
        }
    }

    /**
     * Request background location in a platform-appropriate way:
     * - Android 10 (API 29): request ACCESS_BACKGROUND_LOCATION via the permission launcher (only after foreground granted).
     * - Android 11+ (API 30+): deep-link users to app settings where they can choose "Allow all the time".
     * Docs:
     * - Background location: https://developer.android.com/training/location/permissions#background
     * - Requesting permissions: https://developer.android.com/training/permissions/requesting
     */
    private fun requestBackgroundLocationPermission() {
        when {
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                requestPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Redirect users to app settings to allow "Allow all the time"
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                startActivity(intent)
            }
        }
    }

    private fun startBackgroundLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun startLocationUpdates() {
        try {
            // Get last known location first for immediate feedback
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    sendLocationToServer(location)
                }
            }

            // Set up regular updates
            val locationRequest =
                LocationRequest
                    .Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        3000, // Update every 3 seconds
                    ).build()

            locationCallback =
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        for (loc in locationResult.locations) {
                            currentLocation = loc
                            sendLocationToServer(loc)
                        }
                    }
                }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper(),
            )
        } catch (e: SecurityException) {
            Log.w("MainActivity", "Location permission denied", e)
            // Do not re-trigger permission requests here to avoid nag loops, especially when the user
            // has denied with "Don't ask again". Instead, surface a one-time UI prompt/rationale and
            // provide a path to app settings if needed.
            // Docs: https://developer.android.com/training/permissions/requesting
        }
    }

    private fun sendLocationToServer(location: Location) {
        Log.d(
            "MainActivity",
            "Sending location to server: lat=${location.latitude}, lng=${location.longitude}",
        )
        locationRepository.sendLocationUpdate(location) { success, message ->
            Log.d("MainActivity", "Location update result: success=$success, message=$message")
            if (success) {
                fetchAllLocations() // Fetch all locations after successfully updating our location
            } else {
                runOnUiThread {
                    Toast
                        .makeText(this, "Location update failed: $message", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    /**
     * Fetch locations for display on the map.
     *
     * The FirebaseLocationRepository.getAllLocations() method now handles all the filtering logic:
     * - Gets current user's location from /locations/[myUID]
     * - Gets accepted friends list from /friendships/[myUID]
     * - Gets each friend's location from /locations/[friendUID]
     * - Returns combined map with only user + accepted friends' locations
     *
     * This simplified approach trusts the repository to return properly filtered data,
     * eliminating the need for additional filtering logic in the UI layer.
     */
    private fun fetchAllLocations() {
        locationRepository.getAllLocations { locations ->
            // Repository returns pre-filtered locations (user + accepted friends only)
            // No additional filtering needed - directly update the map display
            allLocations = locations
            Log.d("MainActivity", "Updated map with ${locations.size} locations")
        }
    }

    override fun onPause() {
        super.onPause()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationCallback != null) {
            startLocationUpdates()
        }
    }
}

@Composable
fun MapScreen(
    currentLocation: Location?,
    allLocations: Map<String, LocationModel>,
    onMyLocationClick: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit,
    friendToFocus: String? = null,
    shouldCenterOnMyLocation: Boolean = false,
    onLocationCentered: () -> Unit = {},
) {
    val singapore = LatLng(1.35, 103.87)

    // Default to Singapore if no location available, but will update when location is found
    val initialPosition =
        currentLocation?.let {
            LatLng(it.latitude, it.longitude)
        } ?: singapore

    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(initialPosition, 15f)
        }

    // Auto-center on current location when it becomes available
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            val position = LatLng(location.latitude, location.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(position, 15f)
        }
    }

    // Only focus on friend when specifically requested
    LaunchedEffect(friendToFocus) {
        friendToFocus?.let { friendId ->
            allLocations[friendId]?.let { friendLoc ->
                val position = LatLng(friendLoc.latitude, friendLoc.longitude)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(position, 15f)
            }
        }
    }

    // Center on my location only when explicitly requested
    LaunchedEffect(shouldCenterOnMyLocation) {
        if (shouldCenterOnMyLocation && currentLocation != null) {
            val position = LatLng(currentLocation.latitude, currentLocation.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(position, 15f)
            onLocationCentered()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties =
                MapProperties(
                    isMyLocationEnabled = currentLocation != null,
                ),
            uiSettings =
                MapUiSettings(
                    zoomControlsEnabled = true,
                    zoomGesturesEnabled = true,
                ),
        ) {
            // Show markers for all users from Firebase
            allLocations.forEach { (_, locationData) ->
                val position = LatLng(locationData.latitude, locationData.longitude)
                Marker(
                    state = MarkerState(position = position),
                    title = locationData.displayName,
                    snippet = "Last updated: ${java.util.Date(locationData.timestamp)}",
                )
            }
        }

        // Button layout at the bottom of the screen
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
        ) {
            Button(
                onClick = onMyLocationClick,
                modifier = Modifier.padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Text("My Location", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            MyAccountButton(
                onSignOut = onSignOut,
                onNavigateToFriends = onNavigateToFriends,
            )
        }
    }
}

@Composable
fun MyAccountButton(
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "My Account",
                    modifier =
                        Modifier
                            .size(18.dp)
                            .padding(end = 4.dp),
                )
                Text("My Account")
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Friends") },
                onClick = {
                    expanded = false
                    onNavigateToFriends()
                },
            )
            DropdownMenuItem(
                text = { Text("Sign Out") },
                onClick = {
                    expanded = false
                    onSignOut()
                },
            )
        }
    }
}

enum class Screen {
    MAP,
    FRIENDS,
    ADD_FRIEND,
}
