package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import kotlinx.coroutines.flow.Flow

class GetGuildsUseCase(
    private val repository: UserRepository,
) {
    operator fun invoke(token: String): Flow<Result<List<Guild>>> = repository.getGuilds(token)
}
