package com.test.testing.api

import android.location.Location
import android.util.Log
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseLocationRepository {
    private val TAG = "FirebaseLocationRepo"
    private val database = Firebase.database("https://locshare-93a7b-default-rtdb.europe-west1.firebasedatabase.app/")
    private val locationsRef = database.getReference("locations")
    
    fun sendLocationUpdate(location: Location, userId: String, onResult: (success: Boolean, message: String) -> Unit) {
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis(),
            "accuracy" to location.accuracy,
            "provider" to location.provider
        )
        
        Log.d(TAG, "Attempting to send location: $locationData for user: $userId")
        Log.d(TAG, "Database reference path: ${locationsRef.path}")
        
        locationsRef.child(userId)
            .setValue(locationData)
            .addOnSuccessListener {
                Log.d(TAG, "Location updated successfully for user: $userId")
                onResult(true, "Location updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating location", e)
                onResult(false, "Error updating location: ${e.message}")
            }
    }
    
    fun getAllLocations(onLocationsReceived: (Map<String, LocationModel>) -> Unit) {
        locationsRef.get().addOnSuccessListener { snapshot ->
            val locations = mutableMapOf<String, LocationModel>()
            
            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key ?: continue
                val latitude = userSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                val longitude = userSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                val timestamp = userSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                
                locations[userId] = LocationModel(
                    latitude = latitude,
                    longitude = longitude,
                    userId = userId,
                    timestamp = timestamp
                )
            }
            
            onLocationsReceived(locations)
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error getting locations", e)
            onLocationsReceived(emptyMap())
        }
    }
} 