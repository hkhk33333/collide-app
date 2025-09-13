package com.test.testing.discord.data.datasources

import com.test.testing.discord.cache.CacheManager
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for user-related operations.
 * Handles all caching and local data storage.
 */
@Singleton
class UserLocalDataSource
    @Inject
    constructor(
        private val cacheManager: CacheManager,
    ) {
        fun getCurrentUser(): Flow<Result<User?>> =
            flow {
                try {
                    val cachedUser = cacheManager.get<User>(CacheManager.CacheKey.CURRENT_USER)
                    emit(Result.success(cachedUser))
                } catch (e: Exception) {
                    emit(Result.error(e))
                }
            }

        fun getUsers(): Flow<Result<List<User>>> =
            flow {
                try {
                    val cachedUsers = cacheManager.get<List<User>>(CacheManager.CacheKey.USERS)
                    emit(Result.success(cachedUsers ?: emptyList()))
                } catch (e: Exception) {
                    emit(Result.error(e))
                }
            }

        fun getGuilds(): Flow<Result<List<Guild>>> =
            flow {
                try {
                    val cachedGuilds = cacheManager.get<List<Guild>>(CacheManager.CacheKey.GUILDS)
                    emit(Result.success(cachedGuilds ?: emptyList()))
                } catch (e: Exception) {
                    emit(Result.error(e))
                }
            }

        suspend fun saveCurrentUser(user: User?): Result<Unit> =
            try {
                if (user != null) {
                    cacheManager.put(CacheManager.CacheKey.CURRENT_USER, user)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e)
            }

        suspend fun saveUsers(users: List<User>): Result<Unit> =
            try {
                cacheManager.put(CacheManager.CacheKey.USERS, users)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e)
            }

        suspend fun saveGuilds(guilds: List<Guild>): Result<Unit> =
            try {
                cacheManager.put(CacheManager.CacheKey.GUILDS, guilds)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e)
            }

        suspend fun clearCurrentUser(): Result<Unit> =
            try {
                cacheManager.put(CacheManager.CacheKey.CURRENT_USER, null)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e)
            }

        suspend fun clearAll(): Result<Unit> =
            try {
                // CacheManager doesn't have a clear method, so we'll just remove known keys
                cacheManager.put(CacheManager.CacheKey.CURRENT_USER, null)
                cacheManager.put(CacheManager.CacheKey.USERS, null)
                cacheManager.put(CacheManager.CacheKey.GUILDS, null)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e)
            }
    }
