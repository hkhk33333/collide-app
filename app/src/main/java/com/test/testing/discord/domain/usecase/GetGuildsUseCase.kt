package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetGuildsUseCase(
    private val repository: UserRepository,
) {
    operator fun invoke(
        token: String,
        forceRefresh: Boolean = false,
    ): Flow<Result<List<Guild>>> =
        flow {
            // Domain layer validation
            if (token.isBlank()) {
                emit(Result.error("Token cannot be blank", errorType = ErrorType.CLIENT))
                return@flow
            }

            repository.getGuilds(token, forceRefresh).collect { result ->
                emit(result)
            }
        }
}
