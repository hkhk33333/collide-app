package com.test.testing.discord.models

import com.google.gson.annotations.SerializedName

// Result wrapper for better error handling
sealed class Result<out T> {
    data class Success<out T>(
        val data: T,
    ) : Result<T>()

    data class Error(
        val exception: Exception,
    ) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            is Error -> null
        }

    fun getOrThrow(): T =
        when (this) {
            is Success -> data
            is Error -> throw exception
        }

    fun <R> map(transform: (T) -> R): Result<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }

    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> =
        when (this) {
            is Success -> transform(data)
            is Error -> this
        }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)

        fun error(exception: Exception): Result<Nothing> = Error(exception)

        fun <T> error(message: String): Result<T> = Error(Exception(message))
    }
}

// Corresponds to LocationSchema
data class Location(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Double,
    @SerializedName("desiredAccuracy") val desiredAccuracy: Double,
    // THE ONLY CHANGE IS HERE: Long -> Double
    @SerializedName("lastUpdated") val lastUpdated: Double,
)

// Corresponds to PrivacySettingsSchema
data class PrivacySettings(
    @SerializedName("enabledGuilds") val enabledGuilds: List<String>,
    @SerializedName("blockedUsers") val blockedUsers: List<String>,
)

// Corresponds to DiscordUserSchema
data class DiscordUser(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("avatar") val avatar: String?,
) {
    val avatarUrl: String
        get() =
            if (avatar != null) {
                "https://cdn.discordapp.com/avatars/$id/$avatar.png"
            } else {
                val defaultIndex = (id.toLongOrNull() ?: 0) % 5
                "https://cdn.discordapp.com/embed/avatars/$defaultIndex.png"
            }
}

// Corresponds to UserSchema
data class User(
    @SerializedName("id") val id: String,
    @SerializedName("location") val location: Location?,
    @SerializedName("duser") val duser: DiscordUser,
    @SerializedName("privacy") val privacy: PrivacySettings,
    @SerializedName("pushToken") val pushToken: String?,
    @SerializedName("receiveNearbyNotifications") val receiveNearbyNotifications: Boolean?,
    @SerializedName("allowNearbyNotifications") val allowNearbyNotifications: Boolean?,
    @SerializedName("nearbyNotificationDistance") val nearbyNotificationDistance: Double?,
    @SerializedName("allowNearbyNotificationDistance") val allowNearbyNotificationDistance: Double?,
)

// Corresponds to GuildSchema
data class Guild(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String?,
) {
    val iconUrl: String
        get() =
            if (icon != null) {
                "https://cdn.discordapp.com/icons/$id/$icon.png"
            } else {
                "https://ui-avatars.com/api/?name=${name.take(2)}&background=random"
            }
}

// Corresponds to DiscordTokenResponseSchema
data class DiscordTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("scope") val scope: String,
)

// For POST /token request
data class TokenRequest(
    @SerializedName("code") val code: String,
    @SerializedName("code_verifier") val codeVerifier: String,
    @SerializedName("redirect_uri") val redirectUri: String,
)

// For successful responses like POST /users/me
data class SuccessResponse(
    @SerializedName("success") val success: Boolean,
)
