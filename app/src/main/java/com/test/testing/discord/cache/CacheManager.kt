package com.test.testing.discord.cache

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Professional cache manager for API responses with automatic expiration and memory management
 */
class CacheManager(
    private val context: Context,
) {
    private val gson = Gson()
    private val cacheMutex = Mutex()
    private val cacheDir = File(context.cacheDir, "api_cache")

    init {
        cacheDir.mkdirs()
    }

    /**
     * Cache entry with metadata
     */
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val expirationTime: Long,
        val etag: String? = null,
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expirationTime

        fun isFresh(): Boolean = !isExpired()
    }

    /**
     * Cache different types of data with appropriate expiration times
     */
    enum class CacheKey(
        val key: String,
        val defaultExpirationMinutes: Long,
    ) {
        USERS("users", 5), // 5 minutes for user data
        CURRENT_USER("current_user", 10), // 10 minutes for current user
        GUILDS("guilds", 15), // 15 minutes for guilds (less frequently changing)
    }

    /**
     * Put data in cache
     */
    suspend fun <T> put(
        key: CacheKey,
        data: T,
        customExpirationMinutes: Long? = null,
    ) {
        cacheMutex.withLock {
            try {
                val expirationTime = System.currentTimeMillis() + (customExpirationMinutes ?: key.defaultExpirationMinutes) * 60 * 1000
                val entry = CacheEntry(data, System.currentTimeMillis(), expirationTime)

                val cacheFile = File(cacheDir, key.key)
                cacheFile.writeText(gson.toJson(entry))
            } catch (e: Exception) {
                Log.e("CacheManager", "Failed to cache data for key: ${key.key}", e)
            }
        }
    }

    /**
     * Get data from cache
     */
    suspend fun <T> get(key: CacheKey): T? {
        return cacheMutex.withLock {
            try {
                val cacheFile = File(cacheDir, key.key)
                if (!cacheFile.exists()) return null

                val json = cacheFile.readText()
                val entry: CacheEntry<T> =
                    when (key) {
                        CacheKey.USERS -> {
                            val type = object : TypeToken<CacheEntry<List<User>>>() {}.type
                            gson.fromJson(json, type)
                        }

                        CacheKey.CURRENT_USER -> {
                            val type = object : TypeToken<CacheEntry<User?>>() {}.type
                            gson.fromJson(json, type)
                        }

                        CacheKey.GUILDS -> {
                            val type = object : TypeToken<CacheEntry<List<Guild>>>() {}.type
                            gson.fromJson(json, type)
                        }
                    }

                if (entry.isExpired()) {
                    cacheFile.delete()
                    null
                } else {
                    entry.data
                }
            } catch (e: Exception) {
                Log.e("CacheManager", "Failed to retrieve cached data for key: ${key.key}", e)
                null
            }
        }
    }

    /**
     * Get cached data with Result wrapper
     */
    suspend fun <T> getWithResult(key: CacheKey): Result<T?> =
        try {
            val data = get<T>(key)
            Result.success(data)
        } catch (e: Exception) {
            Result.error(e)
        }

    /**
     * Check if cache has fresh data
     */
    suspend fun hasFreshData(key: CacheKey): Boolean {
        return cacheMutex.withLock {
            try {
                val cacheFile = File(cacheDir, key.key)
                if (!cacheFile.exists()) return false

                val json = cacheFile.readText()
                val entry: CacheEntry<*> =
                    when (key) {
                        CacheKey.USERS -> {
                            val type = object : TypeToken<CacheEntry<List<User>>>() {}.type
                            gson.fromJson(json, type)
                        }

                        CacheKey.CURRENT_USER -> {
                            val type = object : TypeToken<CacheEntry<User?>>() {}.type
                            gson.fromJson(json, type)
                        }

                        CacheKey.GUILDS -> {
                            val type = object : TypeToken<CacheEntry<List<Guild>>>() {}.type
                            gson.fromJson(json, type)
                        }
                    }

                entry.isFresh()
            } catch (e: Exception) {
                Log.e("CacheManager", "Failed to check for fresh data for key: ${key.key}", e)
                false
            }
        }
    }

    /**
     * Invalidate specific cache entry
     */
    suspend fun invalidate(key: CacheKey) {
        cacheMutex.withLock {
            try {
                val cacheFile = File(cacheDir, key.key)
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
            } catch (e: Exception) {
                Log.e("CacheManager", "Failed to invalidate cache for key: ${key.key}", e)
            }
        }
    }

    /**
     * Clear all cache
     */
    suspend fun clearAll() {
        cacheMutex.withLock {
            try {
                cacheDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                Log.e("CacheManager", "Failed to clear cache", e)
            }
        }
    }

    /**
     * Get cache statistics
     */
    suspend fun getStats(): CacheStats =
        cacheMutex.withLock {
            val files = cacheDir.listFiles() ?: emptyArray()
            var totalSize = 0L
            var entryCount = 0

            files.forEach { file ->
                totalSize += file.length()
                entryCount++
            }

            CacheStats(entryCount, totalSize)
        }

    /**
     * Cache statistics
     */
    data class CacheStats(
        val entryCount: Int,
        val totalSizeBytes: Long,
    ) {
        val totalSizeKB: Double get() = totalSizeBytes / 1024.0
        val totalSizeMB: Double get() = totalSizeBytes / (1024.0 * 1024.0)
    }
}
