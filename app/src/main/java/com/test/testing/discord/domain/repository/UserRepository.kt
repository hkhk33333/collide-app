package com.test.testing.discord.domain.repository

import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(token: String): Flow<Result<User?>>

    fun getUsers(token: String): Flow<Result<List<User>>>

    fun getGuilds(token: String): Flow<Result<List<Guild>>>

    suspend fun updateUser(
        token: String,
        user: User,
    ): Result<Unit>

    suspend fun deleteUserData(token: String): Result<Unit>
}
