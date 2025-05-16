package com.test.testing.api

import android.location.Location
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseLocationRepository {
    private val TAG = "FirebaseLocationRepo"
    private val database = Firebase.database("https://locshare-93a7b-default-rtdb.europe-west1.firebasedatabase.app/")
    private val locationsRef = database.getReference("locations")
    private val usersRef = database.getReference("users")
    private val auth = FirebaseAuth.getInstance()
    
    fun sendLocationUpdate(location: Location, onResult: (success: Boolean, message: String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(false, "User not authenticated")
            return
        }
        
        val userId = currentUser.uid
        val userDisplayName = currentUser.displayName ?: "Unknown User"
        
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis(),
            "accuracy" to location.accuracy,
            "provider" to location.provider,
            "displayName" to userDisplayName
        )
        
        Log.d(TAG, "Attempting to send location: $locationData for user: $userId")
        Log.d(TAG, "Database reference path: ${locationsRef.path}")
        
        locationsRef.child(userId)
            .setValue(locationData)
            .addOnSuccessListener {
                // Also update user profile info
                updateUserProfile(currentUser)
                
                Log.d(TAG, "Location updated successfully for user: $userId")
                onResult(true, "Location updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating location", e)
                onResult(false, "Error updating location: ${e.message}")
            }
    }
    
    private fun updateUserProfile(user: FirebaseUser) {
        val userData = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "displayName" to (user.displayName ?: "Unknown User"),
            "lastActive" to System.currentTimeMillis()
        )
        
        usersRef.child(user.uid).setValue(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User profile updated: ${user.uid}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating user profile", e)
            }
    }
    
    fun getAllLocations(onLocationsReceived: (Map<String, LocationModel>) -> Unit) {
        locationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locations = mutableMapOf<String, LocationModel>()
                
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val latitude = userSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = userSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val timestamp = userSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    val displayName = userSnapshot.child("displayName").getValue(String::class.java) ?: "Unknown User"
                    
                    locations[userId] = LocationModel(
                        latitude = latitude,
                        longitude = longitude,
                        userId = userId,
                        timestamp = timestamp,
                        displayName = displayName
                    )
                }
                
                onLocationsReceived(locations)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting locations", error.toException())
                onLocationsReceived(emptyMap())
            }
        })
    }
} 