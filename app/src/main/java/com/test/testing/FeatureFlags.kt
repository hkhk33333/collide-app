package com.test.testing

/**
 * Feature flags for managing the development and staged rollout of the Discord + Backend system.
 *
 * ARCHITECTURE: TWO SEPARATE APPS WITHIN THE SAME CODEBASE
 * - Discord System: Discord OAuth + MySku Backend API (aligns with iOS)
 * - Firebase System: Firebase Auth + Firebase Realtime Database (current default)
 *
 * BuildConfig values (defined per build type in app/build.gradle.kts):
 * - Debug: USE_DISCORD_SYSTEM = false (enable locally as needed)
 * - Release: USE_DISCORD_SYSTEM = false (ship Firebase system only)
 *
 * Notes:
 * - The feature flag must fully gate the Discord system. No UI or logic sharing across systems.
 * - Keep default OFF across variants until the Discord flow is production-ready.
 */
object FeatureFlags {
    /**
     * Master toggle for enabling the Discord + Backend system.
     *
     * This is read from BuildConfig and should be set per build type in Gradle.
     *
     * TRUE (Discord System):
     * - Auth: Discord OAuth 2.0 (PKCE)
     * - Data: MySku Backend API
     * - Features: Guilds, privacy, blocking
     * - UI: Discord-specific navigation and screens
     *
     * FALSE (Firebase System):
     * - Auth: Firebase (email/Google)
     * - Data: Firebase Realtime Database
     * - Features: Friends, location sharing
     * - UI: Firebase-specific navigation and screens
     *
     * IMPORTANT: These are two separate apps within one codebase. Avoid UI mixing.
     */
    val USE_DISCORD_SYSTEM: Boolean
        get() = BuildConfig.USE_DISCORD_SYSTEM
}
