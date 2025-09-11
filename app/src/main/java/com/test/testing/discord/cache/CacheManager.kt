package com.test.testing.discord.cache

import android.content.Context
import com.test.testing.discord.config.AppConfig
import com.test.testing.discord.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Advanced caching system for the Discord app
 * Provides intelligent caching with TTL, size limits, and invalidation strategies
 */
class CacheManager(
    private val context: Context,
    private val maxSizeBytes: Long = AppConfig.CACHE_SIZE_BYTES.toLong(),
    private val defaultTtlMs: Long = AppConfig.CACHE_MAX_AGE_SECONDS.toLong() * 1000L,
) {
    private val cacheDir = File(context.cacheDir, "discord_cache")
    private val cacheMap = mutableMapOf<String, CacheEntry<*>>()
    private val cacheMutex = Mutex()

    // Observable cache statistics
    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: Flow<CacheStats> = _cacheStats

    init {
        cacheDir.mkdirs()
        loadPersistedCache()
        startCleanupScheduler()
    }

    /**
     * Cache entry with metadata
     */
    data class CacheEntry<T>(
        val key: String,
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val ttlMs: Long = AppConfig.CACHE_MAX_AGE_SECONDS.toLong() * 1000L,
        val accessCount: Int = 0,
        val lastAccessed: Long = System.currentTimeMillis(),
        val sizeBytes: Long = 0,
        val tags: Set<String> = emptySet(),
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > ttlMs

        val isStale: Boolean
            get() = System.currentTimeMillis() - lastAccessed > ttlMs / 2
    }

    /**
     * Cache statistics
     */
    data class CacheStats(
        val totalEntries: Int = 0,
        val totalSizeBytes: Long = 0,
        val hitCount: Long = 0,
        val missCount: Long = 0,
        val evictionCount: Long = 0,
        val hitRate: Double = 0.0,
    )

    /**
     * Cache a value with optional TTL and tags
     */
    suspend fun <T> put(
        key: String,
        value: T,
        ttlMs: Long = defaultTtlMs,
        tags: Set<String> = emptySet(),
    ) {
        cacheMutex.withLock {
            val sizeBytes = estimateSize(value)
            ensureCapacity(sizeBytes)

            val entry =
                CacheEntry(
                    key = key,
                    data = value,
                    ttlMs = ttlMs,
                    sizeBytes = sizeBytes,
                    tags = tags,
                )

            @Suppress("UNCHECKED_CAST")
            cacheMap[key] = entry as CacheEntry<*>
            updateStats()
            persistCacheEntry(entry)
        }
    }

    /**
     * Get a cached value
     */
    suspend fun <T> get(key: String): T? =
        cacheMutex.withLock {
            val entry = cacheMap[key]

            val result =
                when {
                    entry == null -> {
                        updateStats(hit = false)
                        null
                    }
                    entry.isExpired -> {
                        cacheMap.remove(key)
                        updateStats(hit = false, eviction = true)
                        null
                    }
                    else -> {
                        // Update access statistics
                        val updatedEntry =
                            entry.copy(
                                accessCount = entry.accessCount + 1,
                                lastAccessed = System.currentTimeMillis(),
                            )
                        cacheMap[key] = updatedEntry
                        updateStats(hit = true)
                        @Suppress("UNCHECKED_CAST")
                        updatedEntry.data as T
                    }
                }

            result
        }

    /**
     * Get a cached value or compute it if not present
     */
    suspend fun <T> getOrPut(
        key: String,
        ttlMs: Long = defaultTtlMs,
        tags: Set<String> = emptySet(),
        compute: suspend () -> T,
    ): T {
        get<T>(key)?.let { return it }

        val value = compute()
        put(key, value, ttlMs, tags)
        return value
    }

    /**
     * Check if a key exists and is not expired
     */
    suspend fun contains(key: String): Boolean =
        cacheMutex.withLock {
            cacheMap[key]?.let { !it.isExpired } ?: false
        }

    /**
     * Remove a specific entry
     */
    suspend fun remove(key: String) {
        cacheMutex.withLock {
            cacheMap.remove(key)
            updateStats()
        }
    }

    /**
     * Clear all entries with specific tags
     */
    suspend fun invalidateByTag(vararg tags: String) {
        cacheMutex.withLock {
            val toRemove =
                cacheMap
                    .filter { (_, entry) ->
                        tags.any { tag -> entry.tags.contains(tag) }
                    }.keys

            toRemove.forEach { cacheMap.remove(it) }
            updateStats()
        }
    }

    /**
     * Clear expired entries
     */
    suspend fun clearExpired() {
        cacheMutex.withLock {
            val expiredKeys = cacheMap.filter { (_, entry) -> entry.isExpired }.keys
            expiredKeys.forEach { cacheMap.remove(it) }
            updateStats(evictions = expiredKeys.size)
        }
    }

    /**
     * Clear all cache entries
     */
    suspend fun clear() {
        cacheMutex.withLock {
            cacheMap.clear()
            updateStats()
            clearPersistedCache()
        }
    }

    /**
     * Get cache entries as Flow for reactive updates
     */
    fun getEntries(): Flow<List<CacheEntry<*>>> = cacheStats.map { cacheMap.values.toList() }

    /**
     * Get entries by tags
     */
    suspend fun getByTags(vararg tags: String): List<CacheEntry<*>> =
        cacheMutex.withLock {
            cacheMap.values.filter { entry ->
                tags.any { tag -> entry.tags.contains(tag) }
            }
        }

    // Private helper methods

    private fun <T> estimateSize(value: T): Long {
        // Simple size estimation - can be enhanced for more accuracy
        return when (value) {
            is String -> value.toByteArray().size.toLong()
            is List<*> -> value.size * 100L // Rough estimate
            is Map<*, *> -> value.size * 150L // Rough estimate
            else -> 1000L // Default estimate
        }
    }

    private suspend fun ensureCapacity(requiredSize: Long) {
        var currentSize = cacheMap.values.sumOf { it.sizeBytes }

        while (currentSize + requiredSize > maxSizeBytes && cacheMap.isNotEmpty()) {
            // Remove least recently used item
            val lruEntry = cacheMap.values.minByOrNull { it.lastAccessed }
            lruEntry?.let {
                cacheMap.remove(it.key)
                currentSize -= it.sizeBytes
                updateStats(eviction = true)
            }
        }
    }

    private fun updateStats(
        hit: Boolean = false,
        eviction: Boolean = false,
        evictions: Int = 0,
    ) {
        val currentStats = _cacheStats.value
        val newHitCount = if (hit) currentStats.hitCount + 1 else currentStats.hitCount
        val newMissCount = if (!hit) currentStats.missCount + 1 else currentStats.missCount
        val newEvictionCount = currentStats.evictionCount + (if (eviction) 1 else 0) + evictions
        val newTotalSize = cacheMap.values.sumOf { it.sizeBytes }
        val totalRequests = newHitCount + newMissCount
        val hitRate = if (totalRequests > 0) newHitCount.toDouble() / totalRequests else 0.0

        _cacheStats.value =
            currentStats.copy(
                totalEntries = cacheMap.size,
                totalSizeBytes = newTotalSize,
                hitCount = newHitCount,
                missCount = newMissCount,
                evictionCount = newEvictionCount,
                hitRate = hitRate,
            )
    }

    private fun startCleanupScheduler() {
        // Schedule periodic cleanup of expired entries
        // In a real implementation, you'd use WorkManager or similar
        Thread {
            while (true) {
                Thread.sleep(300000) // 5 minutes
                // Run in a coroutine since clearExpired is suspend
                kotlinx.coroutines.runBlocking {
                    clearExpired()
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun persistCacheEntry(
        @Suppress("UNUSED_PARAMETER") entry: CacheEntry<*>,
    ) {
        // Persist important cache entries to disk
        // Implementation would depend on your persistence strategy
    }

    private fun loadPersistedCache() {
        // Load persisted cache entries on startup
        // Implementation would depend on your persistence strategy
    }

    private fun clearPersistedCache() {
        // Clear persisted cache
    }
}

/**
 * Cache keys for consistent naming
 */
object CacheKeys {
    const val CURRENT_USER = "current_user"
    const val USERS_LIST = "users_list"
    const val GUILDS_LIST = "guilds_list"
    const val USER_PROFILE = "user_profile"
    const val LOCATION_DATA = "location_data"

    fun userProfile(userId: String) = "user_profile_$userId"

    fun guildUsers(guildId: String) = "guild_users_$guildId"

    fun userLocations(userId: String) = "user_locations_$userId"
}

/**
 * Cache tags for grouping related entries
 */
object CacheTags {
    const val USER_DATA = "user_data"
    const val GUILD_DATA = "guild_data"
    const val LOCATION_DATA = "location_data"
    const val PROFILE_DATA = "profile_data"
    const val TEMPORARY = "temporary"
    const val PERSISTENT = "persistent"
}
