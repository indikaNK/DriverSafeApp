package com.example.driversafeapp_application

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.driversafeapp_application.api.GeoCodingApiService
import com.example.driversafeapp_application.api.GeoCodingResponse
import com.example.driversafeapp_application.api.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RouteInputActivity : AppCompatActivity() {

    private lateinit var geoCodingApiService: GeoCodingApiService
    private val googleMapsApiKey = "AIzaSyB-u93Huo2uyhIVLPsyNxNW7hg5EMb7CsM" // Replace with your API key
    private lateinit var startLocationField: TextInputEditText
    private lateinit var destinationField: TextInputEditText
    private lateinit var weatherDateField: TextInputEditText // Initialized here
    private var startLatLng: LatLng? = null
    private var destLatLng: LatLng? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    //loading screen
    private lateinit var loadingProgress: ProgressBar
    private var pendingTasks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_input)

        // Initialize Geocoding API service
        geoCodingApiService = RetrofitClient.geoCodingApiService

        // Locate UI elements
        startLocationField = findViewById(R.id.startLocationField)
        destinationField = findViewById(R.id.destinationField)
        weatherDateField = findViewById(R.id.weatherDateField) // Added initialization
        val confirmButton = findViewById<Button>(R.id.pickDestination)
        val getCurrentLocationButton = findViewById<Button>(R.id.getCurrentLocationButton)
        val pickWeatherDateButton = findViewById<Button>(R.id.pickWeatherDateButton)
        val clearStartLocationButton = findViewById<Button>(R.id.clearStartLocationButton)
        //loading
        loadingProgress = findViewById(R.id.loadingProgress)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Pre-fill with Intent extra
        val currentLat = intent.getDoubleExtra("currentLocationLat", Double.NaN)
        val currentLng = intent.getDoubleExtra("currentLocationLng", Double.NaN)
        Log.d("RouteInputActivity", "Received currentLocation LAT from Intent: $currentLat")
        Log.d("RouteInputActivity", "Received currentLocation LONG from Intent: $currentLng")
        if (!currentLat.isNaN() && !currentLng.isNaN()) {
            val currentLocation = LatLng(currentLat, currentLng)
            val locationText = "Current Location: Lat ${"%.6f".format(currentLocation.latitude)}, Lng ${"%.6f".format(currentLocation.longitude)}"
            startLocationField.setText(locationText)
            startLatLng = currentLocation // Set as initial value
            Log.d("RouteInputActivity", "Pre-set startLatLng to: $startLatLng")
        } else {
            startLocationField.setText("No current location available")
            Log.w("RouteInputActivity", "No current location provided")
        }

        // Set default weather date to current date
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        weatherDateField.setText(dateFormat.format(calendar.time))
        Log.d("RouteInputActivity", "Default weather date set to: ${weatherDateField.text}")

        // Get Current Location Button (optional refresh)
        getCurrentLocationButton.setOnClickListener {
            if (checkLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }

        pickWeatherDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                weatherDateField.setText(dateFormat.format(selectedCalendar.time))
                Log.d("RouteInputActivity", "Weather date updated to: ${weatherDateField.text}")
            }, year, month, day).show()
        }

        // Clear Start Location Button
        clearStartLocationButton.setOnClickListener {
            startLocationField.text!!.clear()
            startLatLng = null // Reset startLatLng when clearing
            Log.d("RouteInputActivity", "Cleared start location field")
        }

        // Confirm Route Button
        confirmButton.setOnClickListener {
            val startText = startLocationField.text.toString().trim()
            val destText = destinationField.text.toString().trim()
            val weatherDate = weatherDateField.text.toString().trim()

            if (startText.isEmpty() || destText.isEmpty() || weatherDate.isEmpty()) {
                Toast.makeText(this, "Please enter all fields: start, destination, and weather date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Append ", Sri Lanka" to improve geocoding accuracy, use raw coords if unchanged
            val startWithCountry = if (startText.contains("Current Location") || startText.contains("Geocoded Start")) {
                "${currentLat},${currentLng}" // Use raw coords if default
            } else {
                "$startText, Sri Lanka"
            }
            val destWithCountry = "$destText, Sri Lanka"
            Log.d("RouteInputActivity", "Fetching coordinates for start: $startWithCountry, dest: $destWithCountry")

            // Geocode start location
            geoCodingApiService.getCoordinates(startWithCountry, googleMapsApiKey).enqueue(object : Callback<GeoCodingResponse> {
                override fun onResponse(call: Call<GeoCodingResponse>, response: Response<GeoCodingResponse>) {
                    if (response.isSuccessful) {
                        val geoCodingResponse = response.body()
                        if (geoCodingResponse != null && geoCodingResponse.status == "OK" && geoCodingResponse.results.isNotEmpty()) {
                            val location = geoCodingResponse.results[0].geometry.location
                            startLatLng = LatLng(location.lat, location.lng)
                            Log.d("RouteInputActivity", "Geocoded start: $startWithCountry to lat/lng: (${location.lat},${location.lng})")

                            // Geocode destination location
                            geoCodingApiService.getCoordinates(destWithCountry, googleMapsApiKey).enqueue(object : Callback<GeoCodingResponse> {
                                override fun onResponse(call: Call<GeoCodingResponse>, response: Response<GeoCodingResponse>) {
                                    if (response.isSuccessful) {
                                        val geoCodingResponseDest = response.body()
                                        if (geoCodingResponseDest != null && geoCodingResponseDest.status == "OK" && geoCodingResponseDest.results.isNotEmpty()) {
                                            val destLocation = geoCodingResponseDest.results[0].geometry.location
                                            destLatLng = LatLng(destLocation.lat, destLocation.lng)
                                            Log.d("RouteInputActivity", "Geocoded destination: $destWithCountry to lat/lng: (${destLocation.lat},${destLocation.lng})")

                                            // Return coordinates to DashboardActivity
                                            val resultIntent = Intent()
                                            resultIntent.putExtra("startLat", startLatLng!!.latitude)
                                            resultIntent.putExtra("startLng", startLatLng!!.longitude)
                                            resultIntent.putExtra("destLat", destLatLng!!.latitude)
                                            resultIntent.putExtra("destLng", destLatLng!!.longitude)
                                            resultIntent.putExtra("weatherDate", weatherDate)
                                            setResult(RESULT_OK, resultIntent)
                                        } else {
                                            Log.w("RouteInputActivity", "Geocoding failed for destination: status=${geoCodingResponseDest?.status}")
                                            Toast.makeText(this@RouteInputActivity, "Could not find destination: ${geoCodingResponseDest?.status}", Toast.LENGTH_SHORT).show()
                                            setResult(RESULT_CANCELED, Intent())
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                        Log.e("RouteInputActivity", "Geocoding API error for destination: ${response.code()}, errorBody=$errorBody")
                                        Toast.makeText(this@RouteInputActivity, "Geocoding API error for destination: ${response.code()}. Error: $errorBody", Toast.LENGTH_SHORT).show()
                                    }
                                    finish()
                                }

                                override fun onFailure(call: Call<GeoCodingResponse>, t: Throwable) {
                                    Log.e("RouteInputActivity", "Geocoding fetch failed for destination: ${t.message}", t)
                                    Toast.makeText(this@RouteInputActivity, "Failed to fetch destination: ${t.message}", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            })
                        } else {
                            Log.w("RouteInputActivity", "Geocoding failed for start: status=${geoCodingResponse?.status}")
                            Toast.makeText(this@RouteInputActivity, "Could not find start location: ${geoCodingResponse?.status}", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_CANCELED, Intent())
                            finish()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("RouteInputActivity", "Geocoding API error for start: ${response.code()}, errorBody=$errorBody")
                        Toast.makeText(this@RouteInputActivity, "Geocoding API error for start: ${response.code()}. Error: $errorBody", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<GeoCodingResponse>, t: Throwable) {
                    Log.e("RouteInputActivity", "Geocoding fetch failed for start: ${t.message}", t)
                    Toast.makeText(this@RouteInputActivity, "Failed to fetch start location: ${t.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
        }
    }

    private fun showLoading() {
        loadingProgress.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingProgress.visibility = View.GONE
    }

    private fun checkAllTasksComplete() {
        if (pendingTasks <= 0) {
            hideLoading()
        }
    }

    private fun checkLocationPermission(): Boolean {


        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                hideLoading()
            }
        }
    }

    private fun getCurrentLocation() {
        showLoading()
        pendingTasks++ // Increment for location task
        if (checkLocationPermission()) {
            val locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val latLng = LatLng(location.latitude, location.longitude)
                        val locationText = "Current Location: Lat ${"%.6f".format(latLng.latitude)}, Lng ${"%.6f".format(latLng.longitude)}"
                        runOnUiThread {
                            startLocationField.setText(locationText)
                            startLatLng = latLng
                            Log.d("RouteInputActivity", "Updated start location to: $latLng")
                        }
                        fusedLocationClient.removeLocationUpdates(this) // Stop updates after getting location
                        pendingTasks-- // Decrement after successful location fetch
                        checkAllTasksComplete()
                    } ?: run {
                        runOnUiThread {
                            Toast.makeText(this@RouteInputActivity, "Failed to get current location", Toast.LENGTH_SHORT).show()
                        }
                        Log.e("RouteInputActivity", "No location result from FusedLocationProvider")
                        pendingTasks-- // Decrement after successful location fetch
                        checkAllTasksComplete()
                    }
                }
            }, Looper.getMainLooper())
        } else {
            requestLocationPermission()
        }
    }
}