package com.test.testing.discord.viewmodels

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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
 * Extension function to safely execute suspend functions with Result wrapper
 */
suspend fun <T> safeSuspendCall(block: suspend () -> T): com.test.testing.discord.models.Result<T> =
    try {
        val result = block()
        com.test.testing.discord.models.Result
            .success(result)
    } catch (e: Exception) {
        com.test.testing.discord.models.Result
            .error(e)
    }

/**
 * Extension function for Flow to add timeout
 * Note: This is a placeholder - timeout functionality would need to be implemented differently
 */
fun <T> Flow<T>.withTimeout(timeoutMs: Long): Flow<T> = this
