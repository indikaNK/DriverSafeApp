package com.example.driversafeapp_application.api


data class DirectionsResponse (
    val routes: List<Route>,
    val status: String

)

data class Route(
    val overview_polyline: OverviewPolyline,
    val legs: List<Leg>
)
data class OverviewPolyline(
    val points: String
)

data class Leg(
    val start_location: Location,
    val end_location: Location,
    val distance: Distance,
    val duration: Duration
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class Distance(
    val text: String,
    val value: Int
)

data class Duration(
    val text: String,
    val value: Int
)