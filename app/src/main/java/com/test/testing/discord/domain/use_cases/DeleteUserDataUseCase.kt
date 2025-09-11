package com.test.testing.discord.domain.use_cases

import com.test.testing.discord.domain.repository.UserRepository

class DeleteUserDataUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(token: String) {
        repository.deleteUserData(token)
    }
}
