package com.test.testing.discord.auth

/**
 * Interface for providing authentication tokens.
 * This abstracts token access away from specific authentication implementations.
 */
interface TokenProvider {
    /**
     * Gets the current authentication token, formatted as a Bearer token.
     * Returns null if no token is available.
     */
    val token: String?

    /**
     * Gets the raw authentication token without Bearer prefix.
     * Returns null if no token is available.
     */
    val rawToken: String?
}
