package com.test.testing.discord.domain.repository

import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(token: String): Flow<User?>

    fun getUsers(token: String): Flow<List<User>>

    fun getGuilds(token: String): Flow<List<Guild>>

    suspend fun updateUser(
        token: String,
        user: User,
    )

    suspend fun deleteUserData(token: String)
}
