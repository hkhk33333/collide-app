package com.test.testing.discord.network

import com.test.testing.discord.config.AppConfig
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import kotlin.math.min
import kotlin.math.pow

/**
 * Network resilience utilities with retry policies and circuit breaker
 */
class NetworkResilience {
    /**
     * Executes an operation with exponential backoff retry policy
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> Result<T>,
        maxRetries: Int = AppConfig.MAX_RETRY_ATTEMPTS,
        initialDelay: Long = AppConfig.INITIAL_RETRY_DELAY_MS,
        maxDelay: Long = AppConfig.MAX_RETRY_DELAY_MS,
        backoffMultiplier: Double = 2.0,
        @Suppress("UNUSED_PARAMETER") operationName: String = "network_operation",
    ): Result<T> {
        var lastResult: Result<T>? = null
        var currentDelay = initialDelay

        for (attempt in 0..maxRetries) {
            val result = operation()

            // If successful or not retryable, return immediately
            if (shouldReturnEarly(result)) {
                return result
            }

            lastResult = result

            // If this is the last attempt, break out to return final result
            if (attempt == maxRetries) break

            // Calculate delay with jitter to prevent thundering herd
            val delayWithJitter = calculateDelayWithJitter(currentDelay, maxDelay)
            delay(delayWithJitter)

            // Exponential backoff
            currentDelay = min((currentDelay * backoffMultiplier).toLong(), maxDelay)
        }

        return lastResult ?: Result.error(
            message = "All retry attempts failed",
            errorType = ErrorType.UNKNOWN,
        )
    }

    private fun <T> shouldReturnEarly(result: Result<T>): Boolean =
        result.isSuccess ||
            (result is Result.Error && !result.canRetry) ||
            (result is Result.Error && result.errorType !in RETRYABLE_ERROR_TYPES)

    /**
     * Creates a Flow that automatically retries failed operations
     */
    fun <T> Flow<Result<T>>.withResilientRetry(
        maxRetries: Int = AppConfig.MAX_RETRY_ATTEMPTS,
        @Suppress("UNUSED_PARAMETER") initialDelay: Long = AppConfig.INITIAL_RETRY_DELAY_MS,
        @Suppress("UNUSED_PARAMETER") operationName: String = "flow_operation",
    ): Flow<Result<T>> =
        retryWhen { cause, attempt ->
            val shouldRetry = attempt < maxRetries && cause is ResilientException
            if (shouldRetry) {
                val delay =
                    calculateDelayWithJitter(
                        AppConfig.INITIAL_RETRY_DELAY_MS * (2.0.pow(attempt.toDouble())).toLong(),
                        AppConfig.MAX_RETRY_DELAY_MS,
                    )
                delay(delay)
            }
            shouldRetry
        }

    /**
     * Circuit breaker for preventing cascade failures
     */
    class CircuitBreaker(
        private val failureThreshold: Int = 5,
        private val recoveryTimeout: Long = 60000L, // 1 minute
        private val expectedException: (Exception) -> Boolean = { true },
    ) {
        private var failures = 0
        private var lastFailureTime = 0L
        private var state = CircuitState.CLOSED

        enum class CircuitState {
            CLOSED, // Normal operation
            OPEN, // Failing, requests rejected
            HALF_OPEN, // Testing if service recovered
        }

        fun <T> execute(operation: () -> Result<T>): Result<T> {
            when (state) {
                CircuitState.OPEN -> {
                    if (System.currentTimeMillis() - lastFailureTime > recoveryTimeout) {
                        state = CircuitState.HALF_OPEN
                    } else {
                        return Result.error(
                            message = "Circuit breaker is OPEN",
                            errorType = ErrorType.SERVER,
                            canRetry = true,
                        )
                    }
                }
                CircuitState.HALF_OPEN -> {
                    // Allow one request through to test recovery
                }
                CircuitState.CLOSED -> {
                    // Normal operation
                }
            }

            return try {
                val result = operation()
                onSuccess()
                result
            } catch (e: Exception) {
                onFailure(e)
                throw e
            }
        }

        private fun onSuccess() {
            failures = 0
            state = CircuitState.CLOSED
        }

        private fun onFailure(exception: Exception) {
            if (expectedException(exception)) {
                failures++
                lastFailureTime = System.currentTimeMillis()

                if (failures >= failureThreshold) {
                    state = CircuitState.OPEN
                }
            }
        }
    }

    /**
     * Rate limiter to prevent API abuse
     */
    class RateLimiter(
        private val maxRequests: Int = 100,
        private val timeWindowMs: Long = 60000L, // 1 minute
    ) {
        private val requests = mutableListOf<Long>()

        fun isAllowed(): Boolean {
            val now = System.currentTimeMillis()
            requests.removeIf { now - it > timeWindowMs }

            return if (requests.size < maxRequests) {
                requests.add(now)
                true
            } else {
                false
            }
        }

        fun getRemainingRequests(): Int = maxRequests - requests.size

        fun getResetTime(): Long = requests.firstOrNull()?.plus(timeWindowMs) ?: 0L
    }

    companion object {
        private val RETRYABLE_ERROR_TYPES =
            setOf(
                ErrorType.NETWORK,
                ErrorType.TIMEOUT,
                ErrorType.SERVER,
                ErrorType.RATE_LIMITED,
            )

        private fun calculateDelayWithJitter(
            baseDelay: Long,
            maxDelay: Long,
            jitterFactor: Double = 0.1,
        ): Long {
            val jitter = (baseDelay * jitterFactor * (Math.random() * 2 - 1)).toLong()
            return min(baseDelay + jitter, maxDelay).coerceAtLeast(0)
        }
    }
}

/**
 * Exception wrapper for resilient operations
 */
class ResilientException(
    message: String,
    cause: Throwable? = null,
    val canRetry: Boolean = true,
    val errorType: ErrorType = ErrorType.UNKNOWN,
) : Exception(message, cause)

/**
 * Extension functions for Result with resilience
 */
suspend fun <T> Result<T>.retryOnFailure(
    resilience: NetworkResilience,
    operation: suspend () -> Result<T>,
): Result<T> =
    when (this) {
        is Result.Error ->
            if (canRetry) {
                resilience.executeWithRetry(operation)
            } else {
                this
            }
        is Result.Success -> this
    }

/**
 * Factory for creating resilient network operations
 */
object ResilienceFactory {
    private val circuitBreaker = NetworkResilience.CircuitBreaker()
    private val rateLimiter = NetworkResilience.RateLimiter()
    private val resilience = NetworkResilience()

    fun createResilientOperation() =
        ResilientOperation(
            circuitBreaker = circuitBreaker,
            rateLimiter = rateLimiter,
            resilience = resilience,
        )
}

class ResilientOperation(
    private val circuitBreaker: NetworkResilience.CircuitBreaker,
    private val rateLimiter: NetworkResilience.RateLimiter,
    private val resilience: NetworkResilience,
) {
    suspend fun <T> execute(
        operation: suspend () -> Result<T>,
        operationName: String = "resilient_operation",
    ): Result<T> {
        // Check rate limit first
        if (!rateLimiter.isAllowed()) {
            return Result.error(
                message = "Rate limit exceeded",
                errorType = ErrorType.RATE_LIMITED,
                canRetry = true,
                retryAfter = rateLimiter.getResetTime() - System.currentTimeMillis(),
            )
        }

        // Execute through circuit breaker
        return circuitBreaker.execute {
            kotlinx.coroutines.runBlocking {
                resilience.executeWithRetry(operation, operationName = operationName)
            }
        }
    }
}
