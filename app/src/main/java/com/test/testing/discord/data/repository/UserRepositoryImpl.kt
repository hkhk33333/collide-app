package com.test.testing.discord.data.repository

import com.test.testing.discord.data.datasources.UserLocalDataSource
import com.test.testing.discord.data.datasources.UserRemoteDataSource
import com.test.testing.discord.domain.models.DomainGuild
import com.test.testing.discord.domain.models.DomainUser
import com.test.testing.discord.domain.models.mappers.GuildMapper
import com.test.testing.discord.domain.models.mappers.UserMapper
import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: UserRemoteDataSource,
        private val localDataSource: UserLocalDataSource,
        private val userMapper: UserMapper,
        private val guildMapper: GuildMapper,
    ) : UserRepository {
        override fun getCurrentUser(
            token: String,
            forceRefresh: Boolean,
        ): Flow<Result<DomainUser?>> {
            val remoteFlow =
                remoteDataSource
                    .getCurrentUser(token)
                    .map { result ->
                        result.map { dataUser ->
                            dataUser?.let { userMapper.toDomain(it) }
                        }
                    }

            return if (forceRefresh) {
                remoteFlow.flowOn(Dispatchers.IO)
            } else {
                kotlinx.coroutines.flow
                    .flow {
                        // Try cache first
                        localDataSource.getCurrentUser().collect { cacheResult ->
                            if (cacheResult.isSuccess && cacheResult.getOrNull() != null) {
                                val domainUser = cacheResult.getOrNull()?.let { userMapper.toDomain(it) }
                                emit(Result.success(domainUser))
                            }
                        }

                        // Then fetch from network
                        remoteFlow.collect { networkResult ->
                            if (networkResult.isSuccess) {
                                val domainUser = networkResult.getOrNull()
                                if (domainUser != null) {
                                    val dataUser = userMapper.toData(domainUser)
                                    localDataSource.saveCurrentUser(dataUser)
                                }
                            }
                            emit(networkResult)
                        }
                    }.flowOn(Dispatchers.IO)
            }
        }

        override fun getUsers(
            token: String,
            forceRefresh: Boolean,
        ): Flow<Result<List<DomainUser>>> {
            val remoteFlow =
                remoteDataSource
                    .getUsers(token)
                    .map { result ->
                        result.map { dataUsers -> userMapper.toDomainList(dataUsers) }
                    }

            return if (forceRefresh) {
                remoteFlow.flowOn(Dispatchers.IO)
            } else {
                kotlinx.coroutines.flow
                    .flow {
                        // Try cache first
                        localDataSource.getUsers().collect { cacheResult ->
                            if (cacheResult.isSuccess) {
                                val cachedUsers = cacheResult.getOrNull() ?: emptyList()
                                if (cachedUsers.isNotEmpty()) {
                                    emit(Result.success(userMapper.toDomainList(cachedUsers)))
                                }
                            }
                        }

                        // Then fetch from network
                        remoteFlow.collect { networkResult ->
                            if (networkResult.isSuccess) {
                                val domainUsers = networkResult.getOrNull() ?: emptyList()
                                val dataUsers = userMapper.toDataList(domainUsers)
                                localDataSource.saveUsers(dataUsers)
                            }
                            emit(networkResult)
                        }
                    }.flowOn(Dispatchers.IO)
            }
        }

        override fun getGuilds(
            token: String,
            forceRefresh: Boolean,
        ): Flow<Result<List<DomainGuild>>> {
            val remoteFlow =
                remoteDataSource
                    .getGuilds(token)
                    .map { result ->
                        result.map { dataGuilds -> guildMapper.toDomainList(dataGuilds) }
                    }

            return if (forceRefresh) {
                remoteFlow.flowOn(Dispatchers.IO)
            } else {
                kotlinx.coroutines.flow
                    .flow {
                        // Try cache first
                        localDataSource.getGuilds().collect { cacheResult ->
                            if (cacheResult.isSuccess) {
                                val cachedGuilds = cacheResult.getOrNull() ?: emptyList()
                                if (cachedGuilds.isNotEmpty()) {
                                    emit(Result.success(guildMapper.toDomainList(cachedGuilds)))
                                }
                            }
                        }

                        // Then fetch from network
                        remoteFlow.collect { networkResult ->
                            if (networkResult.isSuccess) {
                                val domainGuilds = networkResult.getOrNull() ?: emptyList()
                                val dataGuilds = guildMapper.toDataList(domainGuilds)
                                localDataSource.saveGuilds(dataGuilds)
                            }
                            emit(networkResult)
                        }
                    }.flowOn(Dispatchers.IO)
            }
        }

        override suspend fun updateUser(
            token: String,
            user: DomainUser,
        ): Result<Unit> {
            val dataUser = userMapper.toData(user)
            return remoteDataSource.updateUser(token, dataUser)
        }

        override suspend fun deleteUserData(token: String): Result<Unit> {
            val result = remoteDataSource.deleteUserData(token)
            if (result.isSuccess) {
                localDataSource.clearAll()
            }
            return result
        }
    }
