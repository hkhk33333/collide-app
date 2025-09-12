package com.test.testing.discord.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.test.testing.discord.config.AppConfig
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Professional network resilience layer for handling network issues gracefully
 */
class NetworkResilience private constructor(
    private val context: Context,
) {
    companion object {
        @Volatile
        private var INSTANCE: NetworkResilience? = null

        fun getInstance(context: Context): NetworkResilience =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkResilience(context.applicationContext).also { INSTANCE = it }
            }
    }

    /**
     * Check if device has internet connectivity
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var isConnected = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities != null) {
                isConnected = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            if (networkInfo != null) {
                isConnected = networkInfo.isConnected
            }
        }
        return isConnected
    }

    /**
     * Execute network operation with retry logic and resilience
     */
    @Suppress("ReturnCount")
    suspend fun <T> executeWithResilience(
        operation: suspend () -> Result<T>,
        maxRetries: Int = AppConfig.MAX_RETRY_ATTEMPTS,
        initialDelayMs: Long = AppConfig.INITIAL_RETRY_DELAY_MS,
        maxDelayMs: Long = AppConfig.MAX_RETRY_DELAY_MS,
        operationName: String = "network_operation",
    ): Result<T> {
        if (!isNetworkAvailable()) {
            return Result.error(
                Exception("No internet connection available"),
                errorType = ErrorType.NETWORK,
                canRetry = true,
            )
        }

        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val result = operation()

                // If operation succeeded, return the result
                if (result.isSuccess) {
                    return result
                }

                // If operation failed with a non-retryable error, return immediately
                if (result is Result.Error && !result.canRetry) {
                    return result
                }

                lastException = (result as? Result.Error)?.exception
            } catch (e: Exception) {
                lastException = e

                // Don't retry certain types of errors
                if (isNonRetryableError(e)) {
                    return Result.error(e, errorType = getErrorType(e), canRetry = false)
                }
            }

            // If this isn't the last attempt, wait before retrying
            if (attempt < maxRetries - 1) {
                Log.w("NetworkResilience", "Attempt ${attempt + 1} failed for $operationName, retrying in ${currentDelay}ms")
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
            }
        }

        // All retries exhausted
        return Result.error(
            lastException ?: Exception("Operation failed after $maxRetries attempts"),
            errorType = ErrorType.NETWORK,
            canRetry = true,
        )
    }

    /**
     * Determine if an error is non-retryable
     */
    private fun isNonRetryableError(e: Exception): Boolean =
        when (e) {
            // Authentication errors
            is retrofit2.HttpException -> {
                val code = e.code()
                code == 401 || code == 403 || code == 422
            }

            // Client errors that shouldn't be retried
            is IllegalArgumentException -> {
                true
            }

            // Other specific non-retryable errors
            else -> {
                false
            }
        }

    /**
     * Get error type from exception
     */
    private fun getErrorType(e: Exception): ErrorType =
        when (e) {
            is UnknownHostException -> {
                ErrorType.NETWORK
            }

            is SocketTimeoutException -> {
                ErrorType.TIMEOUT
            }

            is IOException -> {
                ErrorType.NETWORK
            }

            is retrofit2.HttpException -> {
                when (e.code()) {
                    401, 403 -> ErrorType.AUTHENTICATION
                    429 -> ErrorType.RATE_LIMITED
                    in 400..499 -> ErrorType.CLIENT
                    in 500..599 -> ErrorType.SERVER
                    else -> ErrorType.UNKNOWN
                }
            }

            else -> {
                ErrorType.UNKNOWN
            }
        }

    /**
     * OkHttp interceptor for adding resilience headers and logging
     */
    class ResilienceInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            // Add resilience headers
            val newRequest =
                request
                    .newBuilder()
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Accept", "application/json")
                    .build()

            val startTime = System.currentTimeMillis()

            return try {
                val response = chain.proceed(newRequest)
                val duration = System.currentTimeMillis() - startTime

                // Log slow requests
                if (duration > 5000) { // 5 seconds
                    Log.w("NetworkResilience", "Slow request: ${request.url} took ${duration}ms")
                }

                response
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e("NetworkResilience", "Request failed: ${request.url} after ${duration}ms", e)
                throw e
            }
        }
    }
}
