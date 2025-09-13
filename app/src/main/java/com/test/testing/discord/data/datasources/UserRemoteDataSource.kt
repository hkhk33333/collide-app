package com.test.testing.discord.data.datasources

import com.test.testing.discord.api.ApiService
import com.test.testing.discord.models.*
import com.test.testing.discord.network.NetworkResilience
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for user-related operations.
 * Handles all network communication and API calls.
 */
@Singleton
class UserRemoteDataSource
    @Inject
    constructor(
        private val apiService: ApiService,
        private val networkResilience: NetworkResilience,
    ) {
        fun getCurrentUser(token: String): Flow<Result<User?>> =
            flow {
                val result =
                    networkResilience.executeWithResilience(
                        operation = { safeApiCall { apiService.getCurrentUser(token) } },
                        operationName = "getCurrentUser",
                    )
                emit(result)
            }

        fun getUsers(token: String): Flow<Result<List<User>>> =
            flow {
                val result =
                    networkResilience.executeWithResilience(
                        operation = {
                            safeApiCall { apiService.getUsers(token) }.map { it ?: emptyList() }
                        },
                        operationName = "getUsers",
                    )
                emit(result)
            }

        fun getGuilds(token: String): Flow<Result<List<Guild>>> =
            flow {
                val result =
                    networkResilience.executeWithResilience(
                        operation = {
                            safeApiCall { apiService.getGuilds(token) }.map { it ?: emptyList() }
                        },
                        operationName = "getGuilds",
                    )
                emit(result)
            }

        suspend fun updateUser(
            token: String,
            user: User,
        ): Result<Unit> =
            networkResilience.executeWithResilience(
                operation = {
                    safeApiCall { apiService.updateCurrentUser(token, user) }.map { Unit }
                },
                operationName = "updateCurrentUser",
            )

        suspend fun deleteUserData(token: String): Result<Unit> =
            networkResilience.executeWithResilience(
                operation = {
                    safeApiCall { apiService.deleteUserData(token) }.map { Unit }
                },
                operationName = "deleteUserData",
            )

        private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> =
            try {
                val response = apiCall()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Result.error(
                            exception = Exception("Response body is null"),
                            errorType = ErrorType.SERVER,
                            canRetry = true,
                        )
                    }
                } else {
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

                    Result.error(
                        exception = Exception("API Error: ${response.code()} - ${response.message()}"),
                        errorType = errorType,
                        canRetry = canRetry,
                    )
                }
            } catch (e: Exception) {
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
                )
            }
    }
