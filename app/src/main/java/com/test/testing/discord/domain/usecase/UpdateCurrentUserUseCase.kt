package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.User

class UpdateCurrentUserUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(
        token: String,
        user: User,
    ) {
        repository.updateUser(token, user)
    }
}
