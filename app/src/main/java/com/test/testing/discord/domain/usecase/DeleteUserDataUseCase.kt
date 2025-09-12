package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result

class DeleteUserDataUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(token: String): Result<Unit> {
        // Domain layer validation
        if (token.isBlank()) {
            return Result.error("Token cannot be blank", errorType = ErrorType.CLIENT)
        }

        return repository.deleteUserData(token)
    }
}
