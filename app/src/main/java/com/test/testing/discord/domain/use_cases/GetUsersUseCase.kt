package com.test.testing.discord.domain.use_cases

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.User
import kotlinx.coroutines.flow.Flow

class GetUsersUseCase(
    private val repository: UserRepository,
) {
    operator fun invoke(token: String): Flow<List<User>> = repository.getUsers(token)
}
