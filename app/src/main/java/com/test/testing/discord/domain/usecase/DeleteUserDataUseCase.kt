package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.Result

class DeleteUserDataUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(token: String): Result<Unit> = repository.deleteUserData(token)
}
