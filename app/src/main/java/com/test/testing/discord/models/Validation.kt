package com.test.testing.discord.models

/**
 * Data validation utilities for the Discord app
 */
object Validation {
    /**
     * Validates user data
     */
    fun validateUser(user: User): ValidationResult {
        val errors = mutableListOf<String>()

        if (user.id.isBlank()) {
            errors.add("User ID cannot be empty")
        }

        if (user.duser.username.isBlank()) {
            errors.add("Username cannot be empty")
        }

        user.location?.let { location ->
            val locationValidation = validateLocation(location)
            if (locationValidation is ValidationResult.Failure) {
                errors.addAll(locationValidation.errors)
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validates location data
     */
    fun validateLocation(location: Location): ValidationResult {
        val errors = mutableListOf<String>()

        if (location.latitude < -90 || location.latitude > 90) {
            errors.add("Latitude must be between -90 and 90")
        }

        if (location.longitude < -180 || location.longitude > 180) {
            errors.add("Longitude must be between -180 and 180")
        }

        if (location.accuracy < 0) {
            errors.add("Accuracy cannot be negative")
        }

        if (location.desiredAccuracy < 0) {
            errors.add("Desired accuracy cannot be negative")
        }

        if (location.lastUpdated <= 0) {
            errors.add("Last updated timestamp must be positive")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validates guild data
     */
    fun validateGuild(guild: Guild): ValidationResult {
        val errors = mutableListOf<String>()

        if (guild.id.isBlank()) {
            errors.add("Guild ID cannot be empty")
        }

        if (guild.name.isBlank()) {
            errors.add("Guild name cannot be empty")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validates privacy settings
     */
    fun validatePrivacySettings(privacy: PrivacySettings): ValidationResult {
        // Privacy settings are mostly just lists, so basic validation
        val errors = mutableListOf<String>()

        // Could add more sophisticated validation here if needed
        // For example, checking if user IDs in blockedUsers are valid

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validates token format (basic check)
     */
    fun validateToken(token: String): ValidationResult {
        val errors = mutableListOf<String>()

        if (token.isBlank()) {
            errors.add("Token cannot be empty")
        }

        if (token.length < 10) {
            errors.add("Token appears to be too short")
        }

        // Could add more sophisticated JWT validation if needed

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
}

/**
 * Result of validation operation
 */
sealed class ValidationResult {
    object Success : ValidationResult()

    data class Failure(
        val errors: List<String>,
    ) : ValidationResult() {
        val errorMessage: String
            get() = errors.joinToString("\n")
    }

    val isValid: Boolean
        get() = this is Success

    val isInvalid: Boolean
        get() = this is Failure
}

/**
 * Extension functions for validation
 */
fun <T> Result<T>.validate(validator: (T) -> ValidationResult): Result<T> =
    when (this) {
        is Result.Success -> {
            when (val validation = validator(data)) {
                ValidationResult.Success -> this
                is ValidationResult.Failure -> Result.error(Exception(validation.errorMessage))
            }
        }
        is Result.Error -> this
    }
