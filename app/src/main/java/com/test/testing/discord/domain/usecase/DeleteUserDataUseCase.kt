package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result

/**
 * Use case for deleting user data from the Discord service
 *
 * This use case handles:
 * - Authentication token validation
 * - User data deletion operations
 * - Error handling for deletion failures
 * - Data cleanup and privacy compliance
 */
class DeleteUserDataUseCase(
    private val repository: UserRepository,
) {
    /**
     * Deletes the current user's data from Discord
     *
     * @param token Authentication token for API access
     * @return Result indicating success or failure of the deletion
     */
    suspend operator fun invoke(token: String): Result<Unit> {
        // Domain layer validation
        if (token.isBlank()) {
            return Result.error("Token cannot be blank", errorType = ErrorType.CLIENT)
        }

        return repository.deleteUserData(token)
    }
}
