package com.test.testing.api

import android.location.Location
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationRepository {
    companion object {
        private const val TAG = "LocationRepository"
    }

    private val locationApiService = ApiClient.locationApiService

    fun sendLocationUpdate(
        location: Location,
        userId: String,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        val locationModel =
            LocationModel(
                latitude = location.latitude,
                longitude = location.longitude,
                userId = userId,
            )

        locationApiService.updateLocation(locationModel).enqueue(
            object : Callback<LocationResponse> {
                override fun onResponse(
                    call: Call<LocationResponse>,
                    response: Response<LocationResponse>,
                ) {
                    if (response.isSuccessful) {
                        val locationResponse = response.body()
                        if (locationResponse != null) {
                            onResult(locationResponse.success, locationResponse.message)
                        } else {
                            onResult(false, "Empty response")
                        }
                    } else {
                        onResult(false, "Error: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(
                    call: Call<LocationResponse>,
                    t: Throwable,
                ) {
                    Log.e(TAG, "API call failed", t)
                    onResult(false, "Network error: ${t.message}")
                }
            },
        )
    }
}
