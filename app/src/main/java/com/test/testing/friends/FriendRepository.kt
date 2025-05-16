package com.test.testing.friends

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * Repository to manage friend relationships in Firebase
 */
class FriendRepository {
    private val TAG = "FriendRepository"
    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database("https://locshare-93a7b-default-rtdb.europe-west1.firebasedatabase.app/")
    private val friendsRef = database.getReference("friendships")
    private val usersRef = database.getReference("users")
    
    /**
     * Send a friend request to another user
     */
    fun sendFriendRequest(targetUserId: String, onComplete: (success: Boolean, message: String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false, "You need to be logged in")
            return
        }
        
        // Check if user exists
        usersRef.child(targetUserId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                onComplete(false, "User does not exist")
                return@addOnSuccessListener
            }
            
            val currentUserId = currentUser.uid
            
            // Don't allow friending yourself
            if (currentUserId == targetUserId) {
                onComplete(false, "You cannot add yourself as a friend")
                return@addOnSuccessListener
            }
            
            // Create friendship data
            val friendship = FirebaseFriendship(
                status = FriendshipStatus.PENDING.name,
                requestedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Store the friendship in both users' friend lists
            // This is a bidirectional relationship
            friendsRef.child(currentUserId).child(targetUserId)
                .setValue(friendship)
                .addOnSuccessListener {
                    // Now store it in the target user's list too
                    friendsRef.child(targetUserId).child(currentUserId)
                        .setValue(friendship)
                        .addOnSuccessListener {
                            onComplete(true, "Friend request sent")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding reverse friendship", e)
                            onComplete(false, "Failed to send friend request: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding friendship", e)
                    onComplete(false, "Failed to send friend request: ${e.message}")
                }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error checking if user exists", e)
            onComplete(false, "Failed to check if user exists: ${e.message}")
        }
    }
    
    /**
     * Get all friends and pending friends for the current user
     */
    fun getFriendships(onResult: (friendships: List<FriendshipModel>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(emptyList())
            return
        }
        
        val currentUserId = currentUser.uid
        friendsRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendships = mutableListOf<FriendshipModel>()
                
                for (friendSnapshot in snapshot.children) {
                    val friendId = friendSnapshot.key ?: continue
                    val status = friendSnapshot.child("status").getValue(String::class.java) ?: continue
                    val requestedAt = friendSnapshot.child("requestedAt").getValue(Long::class.java) ?: 0L
                    val updatedAt = friendSnapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
                    
                    // Get friend's display name from users collection
                    usersRef.child(friendId).child("displayName").get().addOnSuccessListener { userSnapshot ->
                        val displayName = userSnapshot.getValue(String::class.java) ?: "Unknown"
                        
                        val friendship = FriendshipModel(
                            userId = friendId,
                            displayName = displayName,
                            status = FriendshipStatus.valueOf(status),
                            requestedAt = requestedAt,
                            updatedAt = updatedAt
                        )
                        
                        friendships.add(friendship)
                        
                        // Once we've processed all friends, return the list
                        if (friendships.size == snapshot.childrenCount.toInt()) {
                            onResult(friendships)
                        }
                    }.addOnFailureListener {
                        Log.e(TAG, "Error getting user display name", it)
                        // Still add the friendship even without display name
                        val friendship = FriendshipModel(
                            userId = friendId,
                            displayName = "Unknown",
                            status = FriendshipStatus.valueOf(status),
                            requestedAt = requestedAt,
                            updatedAt = updatedAt
                        )
                        
                        friendships.add(friendship)
                        
                        // Once we've processed all friends, return the list
                        if (friendships.size == snapshot.childrenCount.toInt()) {
                            onResult(friendships)
                        }
                    }
                }
                
                // Handle case of no friends
                if (snapshot.childrenCount == 0L) {
                    onResult(emptyList())
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting friendships", error.toException())
                onResult(emptyList())
            }
        })
    }
    
    /**
     * Accept a friend request
     */
    fun acceptFriendRequest(friendId: String, onComplete: (success: Boolean, message: String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false, "You need to be logged in")
            return
        }
        
        val currentUserId = currentUser.uid
        
        // Update both sides of the friendship
        val updates = mapOf(
            "status" to FriendshipStatus.ACCEPTED.name,
            "updatedAt" to System.currentTimeMillis()
        )
        
        friendsRef.child(currentUserId).child(friendId)
            .updateChildren(updates)
            .addOnSuccessListener {
                friendsRef.child(friendId).child(currentUserId)
                    .updateChildren(updates)
                    .addOnSuccessListener {
                        onComplete(true, "Friend request accepted")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating reverse friendship", e)
                        onComplete(false, "Failed to accept friend request: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating friendship", e)
                onComplete(false, "Failed to accept friend request: ${e.message}")
            }
    }
    
    /**
     * Get the current user's ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get display name for a user ID
     */
    fun getUserDisplayName(userId: String, onResult: (displayName: String) -> Unit) {
        usersRef.child(userId).child("displayName").get()
            .addOnSuccessListener { snapshot ->
                val displayName = snapshot.getValue(String::class.java) ?: "Unknown User"
                onResult(displayName)
            }
            .addOnFailureListener {
                onResult("Unknown User")
            }
    }
} 