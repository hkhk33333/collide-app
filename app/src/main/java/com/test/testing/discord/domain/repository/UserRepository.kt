package com.test.testing.discord.domain.repository

import com.test.testing.discord.domain.models.DomainGuild
import com.test.testing.discord.domain.models.DomainUser
import com.test.testing.discord.models.Result
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(
        token: String,
        forceRefresh: Boolean = false,
    ): Flow<Result<DomainUser?>>

    fun getUsers(
        token: String,
        forceRefresh: Boolean = false,
    ): Flow<Result<List<DomainUser>>>

    fun getGuilds(
        token: String,
        forceRefresh: Boolean = false,
    ): Flow<Result<List<DomainGuild>>>

    suspend fun updateUser(
        token: String,
        user: DomainUser,
    ): Result<Unit>

    suspend fun deleteUserData(token: String): Result<Unit>
}
