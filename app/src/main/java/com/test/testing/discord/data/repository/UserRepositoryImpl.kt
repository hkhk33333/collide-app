package com.test.testing.discord.data.repository

import android.util.Log
import com.test.testing.discord.api.ApiService
import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

class UserRepositoryImpl(
    private val apiService: ApiService,
) : UserRepository {
    override fun getCurrentUser(token: String): Flow<User?> =
        flow {
            emit(handleApiResponse { apiService.getCurrentUser(token) })
        }.flowOn(Dispatchers.IO)

    override fun getUsers(token: String): Flow<List<User>> =
        flow {
            emit(handleApiResponse { apiService.getUsers(token) } ?: emptyList())
        }.flowOn(Dispatchers.IO)

    override fun getGuilds(token: String): Flow<List<Guild>> =
        flow {
            emit(handleApiResponse { apiService.getGuilds(token) } ?: emptyList())
        }.flowOn(Dispatchers.IO)

    override suspend fun updateUser(
        token: String,
        user: User,
    ) {
        handleApiResponse { apiService.updateCurrentUser(token, user) }
    }

    override suspend fun deleteUserData(token: String) {
        try {
            apiService.deleteUserData(token)
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to delete user data", e)
        }
    }

    private suspend fun <T> handleApiResponse(apiCall: suspend () -> Response<T>): T? =
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()
            } else {
                logApiError(response.code())
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "API call failed", e)
            null
        }

    private fun logApiError(code: Int) {
        val errorMessage = "API Error: Code $code"
        Log.e("UserRepository", errorMessage)
    }
}
