package com.example.driversafeapp_application



//maps google

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.text.capitalize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale


class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {


    // get last know location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // google map view
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var locationText: TextView
    private lateinit var speedText: TextView
    private val locationPermissionCode = 100

    //weather view
    private lateinit var weatherText: TextView
    private lateinit var weatherApiService: WeatherApiService
    private val openWeatherApiKey = "a95567bb398bc0266cd2b9e0d0049cae"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize FusedLocationProviderClient
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherApiService = retrofit.create(WeatherApiService::class.java)



        // Initialize MapView
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Initialize TextViews
        locationText = findViewById(R.id.locationText)
        speedText = findViewById(R.id.speedText)
        weatherText = findViewById(R.id.weatherText)



        val startJourneyButton = findViewById<Button>(R.id.startJourneyButton)
        startJourneyButton.setOnClickListener {
            Toast.makeText(this, "Journey started!", Toast.LENGTH_SHORT).show()
            // Add logic for starting the app's main functionality :: start monitoring

            //get location permissions from user
            checkLocationPermission()
        }

        val settingsIconButton = findViewById<ImageButton>(R.id.settingsIconButton)
        settingsIconButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkLocationPermission() {

        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted && coarseLocationGranted) {
            // Permission granted, get location
            getCurrentLocation()
        }else{
            // Request permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show rationale dialog
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs location access to show your position and speed.")
                    .setPositiveButton("OK") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            locationPermissionCode
                        )
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .show()
            } else {
                // Request permission directly
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    locationPermissionCode
                )
            }
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {                // Permission granted
                getCurrentLocation()
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                val speedKmh = location.speed * 3.6 // Convert m/s to km/h
                locationText.text = "Location: Lat ${"%.4f".format(location.latitude)}, Lng ${"%.4f".format(location.longitude)}"
                speedText.text = "Speed: ${"%.1f".format(speedKmh)} km/h"

                // Update map
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(latLng).title("Current Location"))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))



                // Fetch weather data
                fetchWeatherData(location.latitude, location.longitude)


            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error getting location: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // fetch weather from API
    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val call = weatherApiService.getCurrentWeather(latitude, longitude, "metric", openWeatherApiKey)
        call.enqueue(object: Callback<WeatherResponse>{
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    weatherResponse?.let {
                        weatherText.text = "Weather: ${it.weather[0].description.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.ROOT
                            ) else it.toString()
                        }}"
                    } ?: run {
                        weatherText.text = "Weather: Unable to fetch"
                    }
                } else {
                    weatherText.text = "Weather: Error fetching data"
                    Toast.makeText(this@DashboardActivity, "Weather API error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                weatherText.text = "Weather: Network error"
                Toast.makeText(this@DashboardActivity, "Weather fetch failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }

        })
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Enable zoom controls
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    // MapView lifecycle methods
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}