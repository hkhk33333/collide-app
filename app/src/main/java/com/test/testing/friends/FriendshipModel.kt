package com.test.testing.friends

/**
 * Status of a friendship between users
 */
enum class FriendshipStatus {
    PENDING,    // Friend request sent but not accepted
    ACCEPTED,   // Both users are friends
    REJECTED    // Friend request was rejected
}

/**
 * Model for friendship data
 */
data class FriendshipModel(
    val userId: String = "",           // The user ID of the friend
    val displayName: String = "",      // Display name of the friend
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(), // When the request was sent
    val updatedAt: Long = System.currentTimeMillis()    // Last status update
)

/**
 * Model for storing friendship in Firebase
 * This is the structure we'll use in the database
 */
data class FirebaseFriendship(
    val status: String = FriendshipStatus.PENDING.name,
    val requestedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 