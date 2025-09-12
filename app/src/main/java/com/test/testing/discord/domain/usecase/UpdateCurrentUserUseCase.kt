package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User

class UpdateCurrentUserUseCase(
    private val repository: UserRepository,
) {
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
