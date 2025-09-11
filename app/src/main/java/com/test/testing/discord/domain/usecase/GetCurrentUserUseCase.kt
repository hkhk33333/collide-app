package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.User
import kotlinx.coroutines.flow.Flow

class GetCurrentUserUseCase(
    private val repository: UserRepository,
) {
    operator fun invoke(token: String): Flow<User?> = repository.getCurrentUser(token)
}
