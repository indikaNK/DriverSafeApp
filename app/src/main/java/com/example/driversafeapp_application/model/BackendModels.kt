package com.example.driversafeapp_application.model

import com.google.android.gms.maps.model.LatLng

// Request models
data class PredictRequest(
    val Vehicle_Speed: Double,
    val blackspot_distance_m: Double,
    val weather_description: String,
    val hour: Int,
    val day_of_week: Int,
    val month: Int,
    val latitude: Double,
    val longitude: Double
)

data class CheckProximityRequest(
    val lat: Double,
    val lon: Double
)

// Response models
data class PredictResponse(
    val accident_probability: Double,
    val alert: String?
)

data class CheckProximityResponse(
    val alert: Boolean,
    val blackspot: String?,
    val distance: Double?
)