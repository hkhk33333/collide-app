package com.test.testing.discord.domain.usecase

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.Result
import com.test.testing.discord.models.User
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Use case for refreshing all user-related data in parallel
 */
class RefreshUserDataUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(token: String): Result<UserData> =
        coroutineScope {
            try {
                // Fetch all data in parallel
                val currentUserDeferred = async { repository.getCurrentUser(token).first() }
                val usersDeferred = async { repository.getUsers(token).first() }
                val guildsDeferred = async { repository.getGuilds(token).first() }

                val currentUser = currentUserDeferred.await()
                val users = usersDeferred.await()
                val guilds = guildsDeferred.await()

                // Check if all operations succeeded
                val errors = mutableListOf<String>()

                val user =
                    when (currentUser) {
                        is Result.Success -> currentUser.data
                        is Result.Error -> {
                            errors.add("Failed to load current user: ${currentUser.exception.message}")
                            null
                        }
                    }

                val userList =
                    when (users) {
                        is Result.Success -> users.data
                        is Result.Error -> {
                            errors.add("Failed to load users: ${users.exception.message}")
                            emptyList()
                        }
                    }

                val guildList =
                    when (guilds) {
                        is Result.Success -> guilds.data
                        is Result.Error -> {
                            errors.add("Failed to load guilds: ${guilds.exception.message}")
                            emptyList()
                        }
                    }

                if (errors.isNotEmpty()) {
                    Result.error("Multiple errors occurred: ${errors.joinToString(", ")}")
                } else {
                    Result.success(UserData(user, userList, guildList))
                }
            } catch (e: Exception) {
                Result.error(e)
            }
        }

    data class UserData(
        val currentUser: User?,
        val allUsers: List<User>,
        val guilds: List<Guild>,
    )
}
