package com.example.driversafeapp_application.api

import com.example.driversafeapp_application.WeatherApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val WEATHER_BASE_URL = "https://api.openweathermap.org/"
    private const val DIRECTIONS_BASE_URL = "https://maps.googleapis.com/"

    val weatherApiService: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    val directionsApiService: DirectionsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DIRECTIONS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DirectionsApiService::class.java)
    }
}