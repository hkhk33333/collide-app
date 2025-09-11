package com.test.testing.discord.utils

import com.test.testing.discord.models.*
import com.test.testing.discord.network.ResilienceFactory
import com.test.testing.discord.network.ResilientException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/**
 * Advanced error recovery strategies for the Discord app
 */
object ErrorRecoveryStrategies {
    /**
     * User-friendly error messages mapped from technical errors
     */
    fun getUserFriendlyMessage(error: Result.Error): String =
        when (error.errorType) {
            ErrorType.NETWORK -> getNetworkErrorMessage(error)
            ErrorType.AUTHENTICATION -> "Your session has expired. Please log in again."
            ErrorType.AUTHORIZATION -> "You don't have permission to perform this action."
            ErrorType.SERVER -> getServerErrorMessage(error)
            ErrorType.RATE_LIMITED -> getRateLimitErrorMessage(error)
            ErrorType.TIMEOUT -> "Request timed out. Please check your connection and try again."
            else -> "Something went wrong. Please try again."
        }

    private fun getNetworkErrorMessage(error: Result.Error): String =
        when {
            error.exception.message?.contains("timeout", ignoreCase = true) == true ->
                "Connection timed out. Please check your internet connection and try again."
            error.exception.message?.contains("unreachable", ignoreCase = true) == true ->
                "Server is currently unreachable. Please try again later."
            else -> "Network connection issue. Please check your connection and retry."
        }

    private fun getServerErrorMessage(error: Result.Error): String =
        when {
            error.exception.message?.contains("500") == true ->
                "Server is experiencing issues. Please try again later."
            error.exception.message?.contains("503") == true ->
                "Service is temporarily unavailable. Please try again in a few minutes."
            else -> "Server error occurred. Please try again later."
        }

    private fun getRateLimitErrorMessage(error: Result.Error): String {
        val retryAfterMinutes = error.retryAfter?.div(1000)?.div(60) ?: 1
        return "Too many requests. Please wait $retryAfterMinutes minute(s) before trying again."
    }

    /**
     * Determines if an error should trigger a user-facing error state
     */
    fun shouldShowErrorToUser(error: Result.Error): Boolean =
        when (error.errorType) {
            ErrorType.NETWORK, ErrorType.SERVER, ErrorType.TIMEOUT -> true
            ErrorType.AUTHENTICATION, ErrorType.AUTHORIZATION -> true
            ErrorType.RATE_LIMITED -> true
            else -> true // Show all errors by default
        }

    /**
     * Determines if an error should trigger automatic retry
     */
    fun shouldAutoRetry(error: Result.Error): Boolean =
        error.canRetry &&
            when (error.errorType) {
                ErrorType.NETWORK -> true
                ErrorType.TIMEOUT -> true
                ErrorType.SERVER ->
                    error.exception.message?.let { message ->
                        message.contains("500") || message.contains("502") || message.contains("503") || message.contains("504")
                    } ?: false
                ErrorType.RATE_LIMITED -> error.retryAfter != null
                else -> false
            }

    /**
     * Creates a Flow that automatically handles errors and retries
     */
    fun <T> Flow<Result<T>>.withErrorRecovery(): Flow<Result<T>> =
        this
            .retryWhen { cause, attempt ->
                if (cause is ResilientException && attempt < 3) {
                    delay(1000L * attempt) // Exponential backoff
                    true
                } else {
                    false
                }
            }

    /**
     * Enhanced Result handling with recovery strategies
     */
    suspend fun <T> Result<T>.handleWithRecovery(
        onSuccess: suspend (T) -> Unit,
        onError: suspend (Result.Error) -> Unit,
        onRetry: suspend (Result.Error) -> Unit = {},
    ) {
        when (this) {
            is Result.Success -> onSuccess(data)
            is Result.Error -> {
                if (shouldAutoRetry(this)) {
                    onRetry(this)
                } else {
                    onError(this)
                }
            }
        }
    }

    /**
     * Recovery actions based on error type
     */
    fun getRecoveryAction(error: Result.Error): RecoveryAction =
        when (error.errorType) {
            ErrorType.NETWORK -> RecoveryAction.RETRY
            ErrorType.TIMEOUT -> RecoveryAction.RETRY
            ErrorType.SERVER -> RecoveryAction.RETRY_LATER
            ErrorType.AUTHENTICATION -> RecoveryAction.REAUTHENTICATE
            ErrorType.AUTHORIZATION -> RecoveryAction.PERMISSION_DENIED
            ErrorType.RATE_LIMITED -> RecoveryAction.WAIT_AND_RETRY
            else -> RecoveryAction.UNKNOWN
        }

    /**
     * Suggested recovery actions for different error scenarios
     */
    enum class RecoveryAction {
        RETRY, // Simple retry
        RETRY_LATER, // Retry after some time
        WAIT_AND_RETRY, // Wait for rate limit to reset
        REAUTHENTICATE, // User needs to log in again
        PERMISSION_DENIED, // User lacks permissions
        CHECK_SETTINGS, // Check app settings
        CONTACT_SUPPORT, // Contact support
        UNKNOWN, // Unknown error
    }

    /**
     * Error analytics and reporting
     */
    object ErrorAnalytics {
        private val errorCounts = mutableMapOf<String, Int>()
        private val errorTimestamps = mutableMapOf<String, Long>()

        fun trackError(error: Result.Error) {
            val errorKey = "${error.errorType}:${error.exception.message?.take(50) ?: "unknown"}"
            errorCounts[errorKey] = errorCounts.getOrDefault(errorKey, 0) + 1
            errorTimestamps[errorKey] = System.currentTimeMillis()
        }

        fun getErrorStats(): Map<String, Pair<Int, Long>> =
            errorCounts.mapValues { (key, count) ->
                count to (errorTimestamps[key] ?: 0L)
            }

        fun clearStats() {
            errorCounts.clear()
            errorTimestamps.clear()
        }
    }

    /**
     * Graceful degradation strategies
     */
    object GracefulDegradation {
        /**
         * Determines if we should show cached data when fresh data fails
         */
        fun shouldShowCachedData(error: Result.Error): Boolean =
            when (error.errorType) {
                ErrorType.NETWORK -> true
                ErrorType.TIMEOUT -> true
                ErrorType.SERVER -> true
                else -> false
            }

        /**
         * Determines if we should disable certain features when errors occur
         */
        fun getDisabledFeatures(error: Result.Error): List<String> =
            when (error.errorType) {
                ErrorType.SERVER -> listOf("real_time_updates")
                ErrorType.RATE_LIMITED -> listOf("bulk_operations")
                else -> emptyList()
            }
    }
}

/**
 * Extension functions for enhanced Result handling
 */
fun <T> Result<T>.getOrLog(defaultValue: T? = null): T? =
    when (this) {
        is Result.Success -> data
        is Result.Error -> {
            // Log the error for debugging
            println("Error in operation: ${exception.message}")
            ErrorRecoveryStrategies.ErrorAnalytics.trackError(this)
            defaultValue
        }
    }

fun <T> Result<T>.onErrorLog(): Result<T> {
    if (this is Result.Error) {
        ErrorRecoveryStrategies.ErrorAnalytics.trackError(this)
    }
    return this
}

/**
 * Creates a resilient operation with error recovery
 */
suspend fun <T> resilientOperation(
    operation: suspend () -> Result<T>,
    operationName: String = "operation",
): Result<T> {
    val resilientOp = ResilienceFactory.createResilientOperation()
    return resilientOp.execute(operation, operationName)
}
