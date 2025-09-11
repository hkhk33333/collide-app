package com.test.testing.discord.data.repository

import android.util.Log
import com.test.testing.discord.api.ApiService
import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

class UserRepositoryImpl(
    private val apiService: ApiService,
) : UserRepository {
    override fun getCurrentUser(token: String): Flow<Result<User?>> =
        flow {
            emit(safeApiCall { apiService.getCurrentUser(token) })
        }.flowOn(Dispatchers.IO)

    override fun getUsers(token: String): Flow<Result<List<User>>> =
        flow {
            val result = safeApiCall { apiService.getUsers(token) }
            emit(result.map { it ?: emptyList() })
        }.flowOn(Dispatchers.IO)

    override fun getGuilds(token: String): Flow<Result<List<Guild>>> =
        flow {
            val result = safeApiCall { apiService.getGuilds(token) }
            emit(result.map { it ?: emptyList() })
        }.flowOn(Dispatchers.IO)

    override suspend fun updateUser(
        token: String,
        user: User,
    ): Result<Unit> =
        safeApiCall {
            apiService.updateCurrentUser(token, user)
        }.map { Unit }

    override suspend fun deleteUserData(token: String): Result<Unit> =
        safeApiCall {
            apiService.deleteUserData(token)
        }.map { Unit }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> =
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.error("Response body is null")
                }
            } else {
                val errorMessage = "API Error: ${response.code()} - ${response.message()}"
                Log.e("UserRepository", errorMessage)
                Result.error(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "API call failed", e)
            Result.error(e)
        }
}
