package com.test.testing.discord.domain.use_cases

data class UserUseCases(
    val getUsers: GetUsersUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val getGuilds: GetGuildsUseCase,
    val updateCurrentUser: UpdateCurrentUserUseCase,
    val deleteUserData: DeleteUserDataUseCase,
)
