package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case for retrieving Discord guilds/servers
 *
 * This use case handles:
 * - Authentication token validation
 * - Guild data retrieval with caching
 * - Force refresh capability for fresh data
 * - Error handling for guild operations
 */
class GetGuildsUseCase(
    private val repository: UserRepository,
) {
    /**
     * Retrieves Discord guilds/servers for the authenticated user
     *
     * @param token Authentication token for API access
     * @param forceRefresh If true, bypasses cache and fetches fresh data
     * @return Flow of Result containing list of guilds or error
     */
    operator fun invoke(
        token: String,
        forceRefresh: Boolean = false,
    ): Flow<Result<List<Guild>>> =
        flow {
            // Domain layer validation
            if (token.isBlank()) {
                emit(Result.error("Token cannot be blank", errorType = ErrorType.CLIENT))
                return@flow
            }

            repository.getGuilds(token, forceRefresh).collect { result ->
                emit(result)
            }
        }
}
