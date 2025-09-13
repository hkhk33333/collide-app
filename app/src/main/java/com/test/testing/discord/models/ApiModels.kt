package com.test.testing.discord.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// Enhanced Result wrapper for better error handling and recovery
sealed class Result<out T> {
    data class Success<out T>(
        val data: T,
        val metadata: ResultMetadata = ResultMetadata(),
    ) : Result<T>()

    data class Error(
        val exception: Exception,
        val errorType: ErrorType = ErrorType.UNKNOWN,
        val canRetry: Boolean = false,
        val retryAfter: Long? = null,
        val metadata: ResultMetadata = ResultMetadata(),
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

    fun getOrDefault(defaultValue: @UnsafeVariance T): T =
        when (this) {
            is Success -> data
            is Error -> defaultValue
        }

    fun <R> map(transform: (T) -> R): Result<R> =
        when (this) {
            is Success -> Success(transform(data), metadata)
            is Error -> this
        }

    companion object {
        fun <T> success(
            data: T,
            metadata: ResultMetadata = ResultMetadata(),
        ): Result<T> = Success(data, metadata)

        fun error(
            exception: Exception,
            errorType: ErrorType = ErrorType.UNKNOWN,
            canRetry: Boolean = false,
            retryAfter: Long? = null,
            metadata: ResultMetadata = ResultMetadata(),
        ): Result<Nothing> = Error(exception, errorType, canRetry, retryAfter, metadata)

        fun <T> error(
            message: String,
            errorType: ErrorType = ErrorType.UNKNOWN,
            canRetry: Boolean = false,
            retryAfter: Long? = null,
        ): Result<T> = Error(Exception(message), errorType, canRetry, retryAfter)
    }
}

// Error types for better error categorization
enum class ErrorType {
    NETWORK,
    AUTHENTICATION,
    AUTHORIZATION,
    SERVER,
    CLIENT,
    VALIDATION,
    TIMEOUT,
    RATE_LIMITED,
    UNKNOWN,
}

// Metadata for tracking operation details
data class ResultMetadata(
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String? = null,
    val duration: Long? = null,
    val cacheHit: Boolean = false,
)

// Corresponds to LocationSchema
@Parcelize
data class Location(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Double,
    @SerializedName("desiredAccuracy") val desiredAccuracy: Double,
    // THE ONLY CHANGE IS HERE: Long -> Double
    @SerializedName("lastUpdated") val lastUpdated: Double,
) : Parcelable

// Corresponds to PrivacySettingsSchema
@Parcelize
data class PrivacySettings(
    @SerializedName("enabledGuilds") val enabledGuilds: List<String>,
    @SerializedName("blockedUsers") val blockedUsers: List<String>,
) : Parcelable

// Corresponds to DiscordUserSchema
@Parcelize
data class DiscordUser(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("avatar") val avatar: String?,
) : Parcelable {
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
@Parcelize
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
) : Parcelable

// Corresponds to GuildSchema
@Parcelize
data class Guild(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String?,
) : Parcelable {
    val iconUrl: String
        get() =
            if (icon != null) {
                "https://cdn.discordapp.com/icons/$id/$icon.png"
            } else {
                "https://ui-avatars.com/api/?name=${name.take(2)}&background=random"
            }
}

// Corresponds to DiscordTokenResponseSchema
@Parcelize
data class DiscordTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("scope") val scope: String,
) : Parcelable

// For POST /token request
@Parcelize
data class TokenRequest(
    @SerializedName("code") val code: String,
    @SerializedName("code_verifier") val codeVerifier: String,
    @SerializedName("redirect_uri") val redirectUri: String,
) : Parcelable

// For successful responses like POST /users/me
@Parcelize
data class SuccessResponse(
    @SerializedName("success") val success: Boolean,
) : Parcelable
