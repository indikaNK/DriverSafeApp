package com.example.driversafeapp_application.api

// this class is served as a model to get weather api response

data class WeatherResponse(
    val weather: List<Weather>,
    val main: Main,
    val cod: Int // Add cod to check response status
)

data class Weather(
    val description: String
)

data class Main(
    val temp: Double
)