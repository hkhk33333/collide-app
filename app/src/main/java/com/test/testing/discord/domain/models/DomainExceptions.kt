package com.test.testing.discord.domain.models

/**
 * Domain-specific exceptions that encapsulate business logic errors
 */
sealed class DomainException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    // Authentication & Authorization
    class InvalidToken(
        message: String = "Invalid authentication token",
    ) : DomainException(message)

    class TokenExpired(
        message: String = "Authentication token has expired",
    ) : DomainException(message)

    class InsufficientPermissions(
        message: String = "Insufficient permissions for this operation",
    ) : DomainException(message)

    // User-related
    class UserNotFound(
        userId: String,
    ) : DomainException("User not found: $userId")

    class UserBlocked(
        userId: String,
    ) : DomainException("User is blocked: $userId")

    class InvalidUserData(
        field: String,
    ) : DomainException("Invalid user data for field: $field")

    // Location-related
    class InvalidLocation(
        message: String = "Invalid location data",
    ) : DomainException(message)

    class LocationNotAvailable(
        message: String = "Location services not available",
    ) : DomainException(message)

    class LocationPermissionDenied(
        message: String = "Location permission denied",
    ) : DomainException(message)

    // Network-related
    class NetworkError(
        message: String = "Network connection error",
        cause: Throwable? = null,
    ) : DomainException(message, cause)

    class ServerError(
        code: Int,
        message: String = "Server error: $code",
    ) : DomainException(message)

    class TimeoutError(
        message: String = "Request timed out",
    ) : DomainException(message)

    // Business logic
    class PrivacyViolation(
        message: String = "Privacy settings prevent this operation",
    ) : DomainException(message)

    class RateLimitExceeded(
        message: String = "Too many requests, please try again later",
    ) : DomainException(message)

    // Data-related
    class DataNotFound(
        entity: String,
    ) : DomainException("$entity not found")

    class DataIntegrityError(
        message: String = "Data integrity error",
    ) : DomainException(message)
}
