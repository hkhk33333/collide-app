package com.test.testing.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface LocationApiService {
    @POST("update-location")
    fun updateLocation(
        @Body location: LocationModel,
    ): Call<LocationResponse>
}
