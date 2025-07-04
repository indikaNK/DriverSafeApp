package com.example.driversafeapp_application.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val WEATHER_BASE_URL = "https://api.openweathermap.org"
    private const val DIRECTIONS_BASE_URL = "https://maps.googleapis.com/maps/api/"
    private const val GEOCODING_BASE_URL = "https://maps.googleapis.com/maps/api/"
    private const val BACKEND_BASE_URL = "https://362d-2402-d000-8110-1a0b-f593-9fa9-f9e4-32e.ngrok-free.app" // Replace with  URL of ngrok

    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl(WEATHER_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val directionsRetrofit = Retrofit.Builder()
        .baseUrl(DIRECTIONS_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val geocodingRetrofit = Retrofit.Builder()
        .baseUrl(GEOCODING_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val backendRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BACKEND_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherApiService: WeatherApiService by lazy {
        weatherRetrofit.create(WeatherApiService::class.java)
    }
    val directionsApiService: DirectionsApiService by lazy { // Changed 'var' to 'val'
        directionsRetrofit.create(DirectionsApiService::class.java)
    }
    val geoCodingApiService: GeoCodingApiService by lazy {
        geocodingRetrofit.create(GeoCodingApiService::class.java)
    }

    val backendApiService: BackendApiService = backendRetrofit.create(BackendApiService::class.java)

}