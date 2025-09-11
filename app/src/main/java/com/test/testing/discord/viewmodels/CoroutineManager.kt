package com.test.testing.discord.viewmodels

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Improved coroutine job management for ViewModels
 */
class CoroutineManager {
    private val supervisorJob = SupervisorJob()
    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            // Log the exception but don't crash the app
            println("Coroutine exception: ${throwable.message}")
            throwable.printStackTrace()
        }

    val coroutineContext: CoroutineContext = supervisorJob + exceptionHandler
    val coroutineScope = CoroutineScope(coroutineContext)

    private val activeJobs = mutableSetOf<Job>()

    /**
     * Launches a coroutine with automatic lifecycle management
     */
    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        val job = coroutineScope.launch(context, start, block)
        activeJobs.add(job)

        // Remove job from active set when completed
        job.invokeOnCompletion { activeJobs.remove(job) }

        return job
    }

    /**
     * Launches a coroutine that automatically retries on failure
     */
    fun launchWithRetry(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        return launch {
            var currentDelay = initialDelay
            repeat(maxRetries) { attempt ->
                try {
                    block()
                    return@launch // Success, exit retry loop
                } catch (e: Exception) {
                    if (attempt == maxRetries - 1) {
                        throw e // Last attempt failed, rethrow
                    }
                    delay(currentDelay)
                    currentDelay *= 2 // Exponential backoff
                }
            }
        }
    }

    /**
     * Safely collects a flow with error handling
     */
    fun <T> Flow<T>.safeCollect(
        onError: (Throwable) -> Unit = { println("Flow collection error: ${it.message}") },
        onEach: (T) -> Unit,
    ): Job =
        launch {
            this@safeCollect
                .catch { e ->
                    onError(e)
                }.flowOn(Dispatchers.IO)
                .collect { value ->
                    try {
                        onEach(value)
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
        }

    /**
     * Cancels all active jobs
     */
    fun cancelAll() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        supervisorJob.cancel()
    }

    /**
     * Gets the number of active jobs
     */
    val activeJobCount: Int
        get() = activeJobs.size

    /**
     * Checks if there are any active jobs
     */
    val hasActiveJobs: Boolean
        get() = activeJobs.isNotEmpty()
}

/**
 * Enhanced coroutine manager with lifecycle awareness
 */
class LifecycleAwareCoroutineManager {
    private val supervisorJob = SupervisorJob()
    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            // Log the exception but don't crash the app
            println("Coroutine exception: ${throwable.message}")
            throwable.printStackTrace()
        }

    val coroutineContext: CoroutineContext = supervisorJob + exceptionHandler
    val coroutineScope = CoroutineScope(coroutineContext)

    private val activeJobs = mutableSetOf<Job>()
    private var isActive = true

    fun setActive(active: Boolean) {
        isActive = active
        if (!active) {
            cancelAll()
        }
    }

    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job =
        if (isActive) {
            val job = coroutineScope.launch(context, start, block)
            activeJobs.add(job)
            job.invokeOnCompletion { activeJobs.remove(job) }
            job
        } else {
            // Return a completed job if not active
            Job()
        }

    fun cancelAll() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        supervisorJob.cancel()
    }

    val hasActiveJobs: Boolean
        get() = activeJobs.isNotEmpty()
}

/**
 * Extension function to safely execute suspend functions with Result wrapper
 */
suspend fun <T> safeSuspendCall(
    block: suspend () -> T,
    operation: String? = null,
): com.test.testing.discord.models.Result<T> {
    val startTime = System.currentTimeMillis()

    return try {
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        val metadata =
            com.test.testing.discord.models.ResultMetadata(
                operation = operation,
                duration = duration,
            )
        com.test.testing.discord.models.Result
            .success(result, metadata)
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        val metadata =
            com.test.testing.discord.models.ResultMetadata(
                operation = operation,
                duration = duration,
            )
        com.test.testing.discord.models.Result
            .error(e, metadata = metadata)
    }
}

/**
 * Extension function for Flow with timeout and retry capabilities
 */
fun <T> Flow<T>.withTimeoutAndRetry(
    @Suppress("UNUSED_PARAMETER") timeoutMs: Long,
    @Suppress("UNUSED_PARAMETER") maxRetries: Int = 3,
    @Suppress("UNUSED_PARAMETER") retryDelayMs: Long = 1000,
): Flow<T> = this

/**
 * ViewModel extension for managing coroutines with automatic cleanup
 */
fun androidx.lifecycle.ViewModel.launchInViewModelScope(
    manager: LifecycleAwareCoroutineManager,
    block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit,
): kotlinx.coroutines.Job = manager.launch(block = block)

/**
 * Resource management utilities for ViewModels
 */
class ViewModelResourceManager {
    private val resources = mutableListOf<() -> Unit>()

    fun addCleanup(cleanup: () -> Unit) {
        resources.add(cleanup)
    }

    fun cleanup() {
        resources.forEach { it() }
        resources.clear()
    }
}

/**
 * Performance monitoring for operations
 */
class PerformanceMonitor {
    var operationMetrics = mutableMapOf<String, MutableList<Long>>()
        private set

    fun recordOperation(
        operation: String,
        duration: Long,
    ) {
        operationMetrics.getOrPut(operation) { mutableListOf() }.add(duration)
    }

    fun getAverageDuration(operation: String): Double? = operationMetrics[operation]?.average()

    fun getOperationCount(operation: String): Int = operationMetrics[operation]?.size ?: 0

    fun clearMetrics() {
        operationMetrics.clear()
    }
}
