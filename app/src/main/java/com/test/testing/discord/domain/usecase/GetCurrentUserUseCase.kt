package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case for retrieving the current authenticated user
 *
 * This use case handles:
 * - Authentication token validation
 * - Cache management with force refresh option
 * - Current user data retrieval and error handling
 */
class GetCurrentUserUseCase(
    private val repository: UserRepository,
) {
    /**
     * Retrieves the current authenticated user
     *
     * @param token Authentication token for API access
     * @param forceRefresh If true, bypasses cache and fetches fresh data
     * @return Flow of Result containing current user or error
     */
    operator fun invoke(
        token: String,
        forceRefresh: Boolean = false,
    ): Flow<Result<User?>> =
        flow {
            // Domain layer validation
            if (token.isBlank()) {
                emit(Result.error("Token cannot be blank", errorType = ErrorType.CLIENT))
                return@flow
            }

            repository.getCurrentUser(token, forceRefresh).collect { result ->
                emit(result)
            }
        }
}
