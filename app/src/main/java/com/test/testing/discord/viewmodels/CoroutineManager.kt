package com.test.testing.discord.viewmodels

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Simple coroutine manager for ViewModels
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
     * Cancels all active jobs
     */
    fun cancelAll() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        supervisorJob.cancel()
    }
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
