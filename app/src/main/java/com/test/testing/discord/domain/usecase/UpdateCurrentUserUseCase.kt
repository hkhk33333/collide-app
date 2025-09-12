package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User

/**
 * Use case for updating the current user's profile data
 *
 * This use case handles:
 * - Input validation for user data
 * - Authentication token validation
 * - User profile update operations
 * - Error handling for update failures
 */
class UpdateCurrentUserUseCase(
    private val repository: UserRepository,
) {
    /**
     * Updates the current user's profile information
     *
     * @param token Authentication token for API access
     * @param user Updated user data to save
     * @return Result indicating success or failure of the update
     */
    suspend operator fun invoke(
        token: String,
        user: User,
    ): Result<Unit> {
        // Domain layer validation
        if (token.isBlank() || user.id.isBlank()) {
            val errorMessage =
                when {
                    token.isBlank() -> "Token cannot be blank"
                    else -> "User ID cannot be blank"
                }
            return Result.error<Unit>(errorMessage, errorType = ErrorType.CLIENT)
        }

        return repository.updateUser(token, user)
    }
}
