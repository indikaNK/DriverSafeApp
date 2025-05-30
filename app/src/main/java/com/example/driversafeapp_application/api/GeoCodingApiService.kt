package com.example.driversafeapp_application.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GeoCodingApiService {
    @GET("geocode/json")
    fun getCoordinates(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): Call<GeoCodingResponse>
}

data class GeoCodingResponse(
    val results: List<GeoCodingResult>,
    val status: String
)

data class GeoCodingResult(
    val geometry: Geometry
)

data class Geometry(
    val location: Location
)

