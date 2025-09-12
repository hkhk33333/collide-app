package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case for retrieving users from the Discord API
 *
 * This use case handles:
 * - Input validation (token validation)
 * - Cache management (force refresh capability)
 * - Error handling and propagation
 * - Domain layer business logic
 */
class GetUsersUseCase(
    private val repository: UserRepository,
) {
    /**
     * Retrieves users from the Discord API
     *
     * @param token Authentication token for API access
     * @param forceRefresh If true, bypasses cache and fetches fresh data
     * @return Flow of Result containing list of users or error
     */
    operator fun invoke(
        token: String,
        forceRefresh: Boolean = false,
    ): Flow<Result<List<User>>> =
        flow {
            // Domain layer validation
            if (token.isBlank()) {
                emit(Result.error("Token cannot be blank", errorType = ErrorType.CLIENT))
                return@flow
            }

            repository.getUsers(token, forceRefresh).collect { result ->
                emit(result)
            }
        }
}
