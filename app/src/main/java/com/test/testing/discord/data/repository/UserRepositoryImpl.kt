package com.test.testing.discord.data.repository

import android.content.Context
import android.util.Log
import com.test.testing.discord.api.ApiService
import com.test.testing.discord.cache.CacheManager
import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.ResultMetadata
import com.test.testing.discord.models.User
import com.test.testing.discord.network.NetworkResilience
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

class UserRepositoryImpl(
    private val context: Context,
    private val apiService: ApiService,
    private val cacheManager: CacheManager = CacheManager.getInstance(context),
    private val networkResilience: NetworkResilience = NetworkResilience.getInstance(context),
) : UserRepository {
    override fun getCurrentUser(token: String): Flow<Result<User?>> =
        flow {
            // First try to get from cache
            val cachedUser = cacheManager.get<User?>(CacheManager.CacheKey.CURRENT_USER)
            if (cachedUser != null) {
                emit(Result.success(cachedUser))
            }

            // Then try network call with resilience
            val networkResult: Result<User?> =
                networkResilience.executeWithResilience(
                    operation = {
                        safeApiCall(
                            apiCall = { apiService.getCurrentUser(token) },
                            operation = "getCurrentUser",
                        )
                    },
                    operationName = "getCurrentUser",
                )

            if (networkResult.isSuccess) {
                // Cache the successful result
                val userData = networkResult.getOrNull()
                if (userData != null) {
                    cacheManager.put<User?>(CacheManager.CacheKey.CURRENT_USER, userData)
                }
            }

            emit(networkResult)
        }.flowOn(Dispatchers.IO)

    override fun getUsers(token: String): Flow<Result<List<User>>> =
        flow {
            // First try to get from cache
            val cachedUsers = cacheManager.get<List<User>>(CacheManager.CacheKey.USERS)
            if (cachedUsers != null) {
                emit(Result.success(cachedUsers))
            }

            // Then try network call with resilience
            val networkResult =
                networkResilience.executeWithResilience(
                    operation = {
                        val result =
                            safeApiCall(
                                apiCall = { apiService.getUsers(token) },
                                operation = "getUsers",
                            )
                        result.map { it ?: emptyList() }
                    },
                    operationName = "getUsers",
                )

            if (networkResult.isSuccess) {
                // Cache the successful result
                val usersData = networkResult.getOrNull()
                if (usersData != null) {
                    cacheManager.put<List<User>>(CacheManager.CacheKey.USERS, usersData)
                }
            }

            emit(networkResult)
        }.flowOn(Dispatchers.IO)

    override fun getGuilds(token: String): Flow<Result<List<Guild>>> =
        flow {
            // First try to get from cache
            val cachedGuilds = cacheManager.get<List<Guild>>(CacheManager.CacheKey.GUILDS)
            if (cachedGuilds != null) {
                emit(Result.success(cachedGuilds))
            }

            // Then try network call with resilience
            val networkResult =
                networkResilience.executeWithResilience(
                    operation = {
                        val result =
                            safeApiCall(
                                apiCall = { apiService.getGuilds(token) },
                                operation = "getGuilds",
                            )
                        result.map { it ?: emptyList() }
                    },
                    operationName = "getGuilds",
                )

            if (networkResult.isSuccess) {
                // Cache the successful result
                val guildsData = networkResult.getOrNull()
                if (guildsData != null) {
                    cacheManager.put<List<Guild>>(CacheManager.CacheKey.GUILDS, guildsData)
                }
            }

            emit(networkResult)
        }.flowOn(Dispatchers.IO)

    override suspend fun updateUser(
        token: String,
        user: User,
    ): Result<Unit> =
        safeApiCall(
            apiCall = { apiService.updateCurrentUser(token, user) },
            operation = "updateCurrentUser",
        ).map { Unit }

    override suspend fun deleteUserData(token: String): Result<Unit> =
        safeApiCall(
            apiCall = { apiService.deleteUserData(token) },
            operation = "deleteUserData",
        ).map { Unit }

    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>,
        operation: String = "api_call",
    ): Result<T> {
        val startTime = System.currentTimeMillis()

        return try {
            val response = apiCall()
            val duration = System.currentTimeMillis() - startTime
            val metadata =
                ResultMetadata(
                    timestamp = System.currentTimeMillis(),
                    operation = operation,
                    duration = duration,
                )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body, metadata)
                } else {
                    Result.error(
                        exception = Exception("Response body is null"),
                        errorType = ErrorType.SERVER,
                        canRetry = true,
                        metadata = metadata,
                    )
                }
            } else {
                val errorMessage = "API Error: ${response.code()} - ${response.message()}"
                Log.e("UserRepository", errorMessage)

                val errorType =
                    when (response.code()) {
                        400 -> ErrorType.CLIENT
                        401 -> ErrorType.AUTHENTICATION
                        403 -> ErrorType.AUTHORIZATION
                        429 -> ErrorType.RATE_LIMITED
                        in 500..599 -> ErrorType.SERVER
                        else -> ErrorType.UNKNOWN
                    }

                val canRetry = response.code() in listOf(500, 502, 503, 504, 429)
                val retryAfter =
                    if (response.code() == 429) {
                        response.headers()["Retry-After"]?.toLongOrNull()?.times(1000)
                    } else {
                        null
                    }

                Result.error(
                    exception = Exception(errorMessage),
                    errorType = errorType,
                    canRetry = canRetry,
                    retryAfter = retryAfter,
                    metadata = metadata,
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val metadata =
                ResultMetadata(
                    timestamp = System.currentTimeMillis(),
                    operation = operation,
                    duration = duration,
                )

            Log.e("UserRepository", "API call failed", e)

            val errorType =
                when (e) {
                    is java.net.UnknownHostException,
                    is java.net.ConnectException,
                    is java.net.SocketTimeoutException,
                    -> ErrorType.NETWORK
                    else -> ErrorType.UNKNOWN
                }

            Result.error(
                exception = e,
                errorType = errorType,
                canRetry = errorType == ErrorType.NETWORK,
                metadata = metadata,
            )
        }
    }
}
