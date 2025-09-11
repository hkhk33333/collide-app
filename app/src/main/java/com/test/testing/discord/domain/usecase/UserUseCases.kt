package com.test.testing.discord.domain.usecase

data class UserUseCases(
    val getUsers: GetUsersUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val getGuilds: GetGuildsUseCase,
    val updateCurrentUser: UpdateCurrentUserUseCase,
    val deleteUserData: DeleteUserDataUseCase,
)
