package com.example.driversafeapp_application.api


import com.example.driversafeapp_application.model.CheckProximityRequest
import com.example.driversafeapp_application.model.CheckProximityResponse
import com.example.driversafeapp_application.model.PredictRequest
import com.example.driversafeapp_application.model.PredictResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface BackendApiService {
    @POST("/predict")
    fun predictRisk(@Body request: PredictRequest): Call<PredictResponse>

    @POST("/check_proximity")
    fun checkProximity(@Body request: CheckProximityRequest): Call<CheckProximityResponse>
}