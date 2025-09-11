package com.test.testing.discord.domain.use_cases

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.Guild
import kotlinx.coroutines.flow.Flow

class GetGuildsUseCase(
    private val repository: UserRepository,
) {
    operator fun invoke(token: String): Flow<List<Guild>> = repository.getGuilds(token)
}
