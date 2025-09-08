package com.test.testing.discord.api

import com.test.testing.discord.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("token")
    suspend fun exchangeCodeForToken(
        @Body body: TokenRequest,
    ): Response<DiscordTokenResponse>

    @GET("users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String,
    ): Response<User>

    @POST("users/me")
    suspend fun updateCurrentUser(
        @Header("Authorization") token: String,
        @Body user: User,
    ): Response<SuccessResponse>

    @GET("users")
    suspend fun getUsers(
        @Header("Authorization") token: String,
    ): Response<List<User>>

    @GET("guilds")
    suspend fun getGuilds(
        @Header("Authorization") token: String,
    ): Response<List<Guild>>

    @POST("revoke")
    suspend fun revokeToken(
        @Header("Authorization") token: String,
    ): Response<SuccessResponse>

    @DELETE("delete-data")
    suspend fun deleteUserData(
        @Header("Authorization") token: String,
    ): Response<SuccessResponse>
}
