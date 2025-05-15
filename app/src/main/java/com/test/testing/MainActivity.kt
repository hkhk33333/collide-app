package com.test.testing

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.test.testing.ui.theme.TestingTheme
import com.test.testing.api.FirebaseLocationRepository
import com.test.testing.api.LocationModel
import android.widget.Toast
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var currentLocation by mutableStateOf<Location?>(null)
    private val locationRepository = FirebaseLocationRepository()
    private val userId = "user_${System.currentTimeMillis()}" // Generate a unique ID for testing
    private var allLocations by mutableStateOf<Map<String, LocationModel>>(emptyMap())
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (locationPermissionGranted) {
            startLocationUpdates()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermissions()
        
        enableEdgeToEdge()
        setContent {
            TestingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapScreen(
                        currentLocation = currentLocation,
                        allLocations = allLocations,
                        onMyLocationClick = { startLocationUpdates() },
                        onRefreshClick = { fetchAllLocations() }
                    )
                }
            }
        }
    }
    
    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is granted
                startLocationUpdates()
            }
            else -> {
                // Request permissions
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
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
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000 // Update every 3 seconds
            ).build()
            
            locationCallback = object : LocationCallback() {
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
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission issue
        }
    }
    
    private fun sendLocationToServer(location: Location) {
        Log.d("MainActivity", "Sending location to server: lat=${location.latitude}, lng=${location.longitude}, userId=$userId")
        locationRepository.sendLocationUpdate(location, userId) { success, message ->
            Log.d("MainActivity", "Location update result: success=$success, message=$message")
            if (success) {
                fetchAllLocations() // Fetch all locations after successfully updating our location
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Location update failed: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun fetchAllLocations() {
        locationRepository.getAllLocations { locations ->
            allLocations = locations
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
    onRefreshClick: () -> Unit
) {
    val singapore = LatLng(1.35, 103.87)
    val tokyo = LatLng(35.6762, 139.6503)
    
    // Default to Singapore if no location available
    val initialPosition = currentLocation?.let {
        LatLng(it.latitude, it.longitude)
    } ?: singapore
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 15f)
    }
    
    // Force camera update when location changes
    androidx.compose.runtime.LaunchedEffect(currentLocation) {
        currentLocation?.let {
            val position = LatLng(it.latitude, it.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(position, 15f)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = currentLocation != null
            )
        ) {
            // Show marker at Singapore (reference point)
            Marker(
                state = MarkerState(position = singapore),
                title = "Singapore",
                snippet = "Marker in Singapore"
            )
            
            // Show markers for all users from Firebase
            allLocations.forEach { (userId, locationData) ->
                val position = LatLng(locationData.latitude, locationData.longitude)
                Marker(
                    state = MarkerState(position = position),
                    title = "User: $userId",
                    snippet = "Last updated: ${java.util.Date(locationData.timestamp)}"
                )
            }
        }
        
        // Button layout at the bottom of the screen
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Button(
                onClick = { onRefreshClick() },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Refresh Locations")
            }
            
            Button(
                onClick = { 
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(tokyo, 10f)
                }
            ) {
                Text("Go to Tokyo")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onMyLocationClick
            ) {
                Text("My Location")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    TestingTheme {
        MapScreen(currentLocation = null, allLocations = emptyMap(), onMyLocationClick = {}, onRefreshClick = {})
    }
} 