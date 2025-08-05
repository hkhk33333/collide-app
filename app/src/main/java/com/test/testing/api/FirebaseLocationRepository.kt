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
    
    /**
     * Get locations for current user and all accepted friends.
     * 
     * Firebase structure: /locations/[uid] contains each user's individual location
     * Firebase rules: Can read own location + friends' locations if friendship status is ACCEPTED
     * 
     * Process:
     * 1. Get current user's location from /locations/[myUID]
     * 2. Get list of accepted friends from /friendships/[myUID]
     * 3. For each accepted friend, get their location from /locations/[friendUID]
     * 4. Combine all locations into single map for display
     */
    fun getAllLocations(onLocationsReceived: (Map<String, LocationModel>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user - cannot get locations")
            onLocationsReceived(emptyMap())
            return
        }
        
        val locations = mutableMapOf<String, LocationModel>()
        val currentUserId = currentUser.uid
        val friendsRef = database.getReference("friendships")
        
        // Remove existing listener if any
        friendshipListener?.let { listener ->
            friendsRef.child(currentUserId).removeEventListener(listener)
        }
        
        Log.d(TAG, "Getting locations for user: $currentUserId and their accepted friends")
        
        // Step 1: Get my own location from /locations/[myUID]
        locationsRef.child(currentUserId).get().addOnSuccessListener { mySnapshot ->
            if (mySnapshot.exists()) {
                val myLocation = parseLocationFromSnapshot(mySnapshot, currentUserId)
                locations[currentUserId] = myLocation
                Log.d(TAG, "Found my location: ${myLocation.displayName}")
            } else {
                Log.d(TAG, "No location found for current user")
            }
            
            // Step 2: Get accepted friends list from /friendships/[myUID]
            friendsRef.child(currentUserId).get().addOnSuccessListener { friendshipsSnapshot ->
                val acceptedFriends = mutableListOf<String>()
                
                // Parse friendships to find accepted ones
                for (friendSnapshot in friendshipsSnapshot.children) {
                    val friendId = friendSnapshot.key ?: continue
                    val status = friendSnapshot.child("status").getValue(String::class.java)
                    if (status == "ACCEPTED") {
                        acceptedFriends.add(friendId)
                    }
                }
                
                Log.d(TAG, "Found ${acceptedFriends.size} accepted friends")
                
                if (acceptedFriends.isEmpty()) {
                    // No friends - return just my location
                    Log.d(TAG, "No accepted friends, returning only my location")
                    onLocationsReceived(locations)
                    return@addOnSuccessListener
                }
                
                // Step 3: Get each friend's location from /locations/[friendUID]
                var processedFriends = 0
                
                acceptedFriends.forEach { friendId ->
                    locationsRef.child(friendId).get().addOnSuccessListener { friendSnapshot ->
                        if (friendSnapshot.exists()) {
                            val friendLocation = parseLocationFromSnapshot(friendSnapshot, friendId)
                            locations[friendId] = friendLocation
                            Log.d(TAG, "Found friend location: ${friendLocation.displayName}")
                        } else {
                            Log.d(TAG, "No location found for friend: $friendId")
                        }
                        
                        processedFriends++
                        
                        // Step 4: Return results when all friends processed
                        if (processedFriends == acceptedFriends.size) {
                            Log.d(TAG, "Completed loading ${locations.size} total locations")
                            onLocationsReceived(locations)
                        }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error getting friend location for $friendId", e)
                        processedFriends++
                        
                        // Continue even if some friend locations fail
                        if (processedFriends == acceptedFriends.size) {
                            Log.d(TAG, "Completed loading ${locations.size} total locations (with some failures)")
                            onLocationsReceived(locations)
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error getting friendships list", e)
                // Return at least my location if friendships query fails
                Log.d(TAG, "Friendships query failed, returning only my location")
                onLocationsReceived(locations)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error getting my location", e)
            onLocationsReceived(emptyMap())
        }
    }
    
    /**
     * Parse location data from Firebase snapshot into LocationModel
     * 
     * @param snapshot Firebase DataSnapshot containing location data
     * @param userId User ID to associate with the location
     * @return LocationModel with parsed data and fallback values for missing fields
     */
    private fun parseLocationFromSnapshot(snapshot: DataSnapshot, userId: String): LocationModel {
        val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
        val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
        val displayName = snapshot.child("displayName").getValue(String::class.java) ?: "Unknown User"
        
        return LocationModel(
            latitude = latitude,
            longitude = longitude,
            userId = userId,
            timestamp = timestamp,
            displayName = displayName
        )
    }
} 