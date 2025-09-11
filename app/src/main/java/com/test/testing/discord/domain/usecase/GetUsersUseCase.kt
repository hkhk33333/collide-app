package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User
import kotlinx.coroutines.flow.Flow

class GetUsersUseCase(
    private val repository: UserRepository,
) {
    operator fun invoke(token: String): Flow<Result<List<User>>> = repository.getUsers(token)
}
