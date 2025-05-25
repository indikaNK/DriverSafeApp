package com.example.driversafeapp_application

// this class is served as a model to get weather api response
data class WeatherResponse(

    val weather: List<Weather>,
    val main: Main


)

data class Weather(
    val description: String,
    val icon: String
)

data class Main(
    val temp: Float
)