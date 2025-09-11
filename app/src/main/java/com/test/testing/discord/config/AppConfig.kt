package com.test.testing.discord.config

import com.test.testing.BuildConfig

/**
 * Application configuration management
 * Provides centralized access to app-wide settings and environment variables
 */
object AppConfig {
    // API Configuration - Single source of truth from BuildConfig
    val baseUrl: String = BuildConfig.DISCORD_BACKEND_URL
    val discordClientId: String = BuildConfig.DISCORD_CLIENT_ID
    const val CALLBACK_URL: String = "mysku://redirect"

    // Network Configuration
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // Retry Configuration
    const val MAX_RETRY_ATTEMPTS = 3
    const val INITIAL_RETRY_DELAY_MS = 1000L
    const val MAX_RETRY_DELAY_MS = 30000L

    // Cache Configuration
    const val CACHE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB
    const val CACHE_MAX_AGE_SECONDS = 60 * 60 * 24 * 7 // 7 days

    // Location Configuration
    const val DEFAULT_UPDATE_INTERVAL_MS = 60000L // 1 minute
    const val FAST_UPDATE_INTERVAL_MS = 15000L // 15 seconds
    const val MIN_UPDATE_INTERVAL_MS = 10000L // 10 seconds
    const val DEFAULT_MIN_MOVEMENT_METERS = 1000f // 1km
    const val FAST_MIN_MOVEMENT_METERS = 100f // 100m

    // UI Configuration
    const val DEFAULT_ANIMATION_DURATION_MS = 300L
    const val SNACKBAR_DURATION_MS = 4000L

    // Data Refresh Configuration
    const val USER_DATA_REFRESH_INTERVAL_MS = 30000L // 30 seconds
    const val LOCATION_DATA_REFRESH_INTERVAL_MS = 60000L // 1 minute

    // Build Type Checks
    val isDebug: Boolean = BuildConfig.DEBUG
    val isRelease: Boolean = !BuildConfig.DEBUG
    val isDiscordSystemEnabled: Boolean = BuildConfig.USE_DISCORD_SYSTEM

    // Feature Flags - Environment aware
    object Features {
        val ENABLE_DETAILED_LOGGING = isDebug
        val ENABLE_CRASH_REPORTING = !isDebug
        val ENABLE_ANALYTICS = !isDebug
    }

    // Environment-specific settings
    object Environment {
        val name: String =
            when {
                baseUrl.contains("staging") -> "staging"
                baseUrl.contains("dev") -> "development"
                baseUrl.contains("localhost") -> "development"
                else -> "production"
            }

        val isProduction: Boolean = name == "production"
        val isStaging: Boolean = name == "staging"
        val isDevelopment: Boolean = name == "development"

        // Environment-specific configurations
        val enableMockData: Boolean = isDevelopment
        val enableNetworkLogging: Boolean = !isProduction
        val enableStrictMode: Boolean = isDevelopment
        val enableLeakCanary: Boolean = isDevelopment
        val crashReportingEnabled: Boolean = isProduction || isStaging
        val analyticsEnabled: Boolean = isProduction
    }

    // Logging Configuration
    object Logging {
        const val ENABLE_NETWORK_LOGGING = true
        const val ENABLE_CRASH_LOGGING = true
        const val LOG_LEVEL = "DEBUG" // DEBUG, INFO, WARN, ERROR
    }
}
