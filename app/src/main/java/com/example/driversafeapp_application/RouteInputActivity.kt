package com.example.driversafeapp_application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driversafeapp_application.api.GeoCodingApiService
import com.example.driversafeapp_application.api.GeoCodingResponse
import com.example.driversafeapp_application.api.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.driversafeapp_application.api.Location

class RouteInputActivity : AppCompatActivity() {
    private lateinit var geoCodingApiService: GeoCodingApiService
    private val googleMapsApiKey = "AIzaSyB-u93Huo2uyhIVLPsyNxNW7hg5EMb7CsM" // Replace with your API key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_input)

        // Initialize Geocoding API service
        geoCodingApiService = RetrofitClient.geoCodingApiService

        // Locate UI elements
        val destinationField = findViewById<TextInputEditText>(R.id.destinationField)
        val startJourneyButton = findViewById<Button>(R.id.startJourneyButton)

        // Start Journey Button
        startJourneyButton.setOnClickListener {
            val destination = destinationField.text.toString().trim()

            if (destination.isEmpty()) {
                Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Append ", Sri Lanka" to improve geocoding accuracy
            val destinationWithCountry = "$destination, Sri Lanka"
            Log.d("RouteInputActivity", "Fetching coordinates for destination: $destinationWithCountry")

            // Fetch coordinates using Geocoding API
            geoCodingApiService.getCoordinates(destinationWithCountry, googleMapsApiKey).enqueue(object : Callback<GeoCodingResponse> {
                override fun onResponse(call: Call<GeoCodingResponse>, response: Response<GeoCodingResponse>) {
                    if (response.isSuccessful) {
                        val geoCodingResponse = response.body()
                        if (geoCodingResponse != null) {
                            Log.d("RouteInputActivity", "Geocoding API response status: ${geoCodingResponse.status}")
                            Log.d("RouteInputActivity", "Geocoding API response results: ${geoCodingResponse.results}")
                            if (geoCodingResponse.status == "OK" && geoCodingResponse.results.isNotEmpty()) {
                                val location = geoCodingResponse.results[0].geometry.location
                                Log.d("RouteInputActivity", "Geocoded destination: $destinationWithCountry to lat/lng: (${location.lat},${location.lng})")

                                // Return the coordinates to DashboardActivity
                                val resultIntent = Intent()
                                resultIntent.putExtra("destinationLat", location.lat)
                                resultIntent.putExtra("destinationLng", location.lng)
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            } else {
                                Log.w("RouteInputActivity", "Geocoding failed: status=${geoCodingResponse.status}")
                                Toast.makeText(this@RouteInputActivity, "Could not find location: ${geoCodingResponse.status}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("RouteInputActivity", "Geocoding response body is null")
                            Toast.makeText(this@RouteInputActivity, "Geocoding failed: No response data", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("RouteInputActivity", "Geocoding API error: ${response.code()}, errorBody=$errorBody")
                        Toast.makeText(this@RouteInputActivity, "Geocoding API error: ${response.code()}. Error: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GeoCodingResponse>, t: Throwable) {
                    Log.e("RouteInputActivity", "Geocoding fetch failed: ${t.message}", t)
                    Toast.makeText(this@RouteInputActivity, "Failed to fetch location: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}