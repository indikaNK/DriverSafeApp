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
import com.example.driversafeapp_application.api.DirectionsResponse
import com.example.driversafeapp_application.api.RetrofitClient.directionsApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import android.graphics.Color
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



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
    private  var googleMap: GoogleMap? = null
    private lateinit var locationText: TextView
    private lateinit var speedText: TextView

    private val locationPermissionCode = 100
    private var currentLocation: LatLng? = null
    private var destinationLocation: LatLng? = null
    private var isMapReady = false
    private var pendingRouteRequest: Pair<LatLng, LatLng>? = null
    private var isLocationReady = false
    private val mawanellaLocation = LatLng(7.252431177548869, 80.44684581600977) // Mawanella as destination 7.252431177548869, 80.44684581600977


    private lateinit var pickRouteButton: Button


    //weather view
    private lateinit var weatherText: TextView
    private lateinit var weatherApiService: WeatherApiService
    private val openWeatherApiKey = "a95567bb398bc0266cd2b9e0d0049cae"
    private val googleMapsApiKey = "AIzaSyB-u93Huo2uyhIVLPsyNxNW7hg5EMb7CsM"


    //firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    private val routeInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        Log.d("DashboardActivity", "Received result from RouteInputActivity: resultCode=${result.resultCode}")

        if (result.resultCode == RESULT_OK) {

            val destination = result.data?.getStringExtra("destination")
            if (destination == null) {
                Log.e("DashboardActivity", "Destination is null in route input result")
                Toast.makeText(this, "No destination provided", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            Log.d("DashboardActivity", "Destination received: $destination")
            // For testing, set Mawanella as the destination
            destinationLocation = mawanellaLocation
            Log.d("DashboardActivity", "Current location state: $currentLocation")
            if (currentLocation != null && destinationLocation != null) {
                if (isMapReady && googleMap != null) {

                    // !! not null
                    fetchRoute(currentLocation!!, destinationLocation!!)
                } else {
                    //error handler:: route request get stored until Map is ready, added as a
                    pendingRouteRequest = Pair(currentLocation!!, destinationLocation!!)
                    Log.d("DashboardActivity", "Map not ready, storing route request: $pendingRouteRequest")
                }
            } else {
                Log.e("DashboardActivity", "Cannot fetch route: currentLocation=$currentLocation, destinationLocation=$destinationLocation")
                Toast.makeText(this, "Location data not available, please wait and try again", Toast.LENGTH_SHORT).show()
            }
        } else {
//            warn log
            Log.w("DashboardActivity", "Route input cancelled or failed: resultCode=${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)


        // Error handler:: Check for Google Play Services availability
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                Toast.makeText(this, "Google Play Services is not available on this device", Toast.LENGTH_LONG).show()
                finish()
            }
            return
        }
        Log.d("DashboardActivity", "Google Play Services available: version=${GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE}")


        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()



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

         // Settings Icon Button
        val settingsIconButton = findViewById<ImageButton>(R.id.settingsIconButton)
        settingsIconButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        pickRouteButton = findViewById<Button>(R.id.pickRouteButton)

        pickRouteButton.isEnabled = false  // Disable until location is ready

        pickRouteButton.setOnClickListener {

            Log.d("DashboardActivity", "Pick Route clicked, currentLocation: $currentLocation")

            routeInputLauncher.launch(Intent(this, RouteInputActivity::class.java))

            Toast.makeText(this, "Lets Pick a Location to Go !", Toast.LENGTH_SHORT).show()
            // Add logic for starting the app's main functionality :: start monitoring

            //get location permissions from user
            // Automatically fetch location, weather, and speed on start

        }

        // Restore state if available
        if (savedInstanceState != null) {
            val lat = savedInstanceState.getDouble("currentLat", -1.0)
            val lng = savedInstanceState.getDouble("currentLng", -1.0)
            if (lat != -1.0 && lng != -1.0) {
                currentLocation = LatLng(lat, lng)
                isLocationReady = true
                pickRouteButton.isEnabled = true
                Log.d("DashboardActivity", "Restored currentLocation: $currentLocation")
            }

        }
// Automatically fetch location, weather, and speed on start
        Log.d("DashboardActivity", "Checking location permissions on create")
        checkLocationPermission()

    }

//saves currentLocation’s latitude and longitude in a Bundle before the activity is destroyed
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentLocation?.let {
            outState.putDouble("currentLat", it.latitude)
            outState.putDouble("currentLng", it.longitude)
            Log.d("DashboardActivity", "Saved currentLocation: $currentLocation")
        }
    }
//restores currentLocation from the Bundle when the activity is recreated
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val lat = savedInstanceState.getDouble("currentLat", -1.0)
        val lng = savedInstanceState.getDouble("currentLng", -1.0)
        if (lat != -1.0 && lng != -1.0) {
            currentLocation = LatLng(lat, lng)
            isLocationReady = true
            pickRouteButton.isEnabled = true
            Log.d("DashboardActivity", "Restored currentLocation onRestore: $currentLocation")
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
        Log.d("DashboardActivity", "onRequestPermissionsResult: requestCode=$requestCode, grantResults=${grantResults.contentToString()}")
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Both permissions granted
                Log.d("DashboardActivity", "Location permissions granted, fetching location")
                getCurrentLocation()
            } else {
                // Permission denied
                Log.w("DashboardActivity", "Location permissions denied")
                Toast.makeText(this, "Location permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }



        // Fetch blackspots
        fetchBlackspots()

        // Commented for testing
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {

                val currentLatLng = LatLng(location.latitude, location.longitude)
                val latLng = LatLng(location.latitude, location.longitude)
                val speedKmh = location.speed * 3.6 // Convert m/s to km/h
                locationText.text = "Location: Lat ${"%.4f".format(location.latitude)}, Lng ${"%.4f".format(location.longitude)}"
                speedText.text = "Speed: ${"%.1f".format(speedKmh)} km/h"

                currentLocation = currentLatLng
                isLocationReady = true
                Log.d("DashboardActivity", "Real location set: $currentLocation, enabling Pick Route button")
                pickRouteButton.isEnabled = true  // Enable button now that location is ready




// Update map with current location if map is ready
                if (isMapReady && googleMap != null) {
                    googleMap?.clear()
                    googleMap?.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    Log.w("DashboardActivity", "Map not ready when updating current location")
                }

                // Fetch weather data
                fetchWeatherData(location.latitude, location.longitude)

                // Fetch blackspots
                fetchBlackspots()


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


    private fun fetchRoute(start: LatLng, end: LatLng) {
        if (!isMapReady || googleMap == null) {
            Log.e("DashboardActivity", "Cannot fetch route: Map not ready")
            Toast.makeText(this, "Map not ready, please try again", Toast.LENGTH_SHORT).show()
            return
        }

        val origin = "${start.latitude},${start.longitude}"
        val destination = "${end.latitude},${end.longitude}"
        Log.d("DashboardActivity", "Fetching route from $origin to $destination")
        directionsApiService.getDirections(origin, destination, googleMapsApiKey).enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()
                    if (directionsResponse?.status == "OK" && directionsResponse.routes.isNotEmpty()) {
                        val route = directionsResponse.routes[0]
                        val points = route.overview_polyline.points
                        Log.d("DashboardActivity", "Encoded polyline points: $points")
                        try {
                            val polylinePoints = PolyUtil.decode(points ?: throw IllegalArgumentException("Polyline points are null"))
                            Log.d("DashboardActivity", "Decoded polyline points count: ${polylinePoints.size}")
                            if (polylinePoints.isNotEmpty()) {
                                val polylineOptions = PolylineOptions()
                                    .addAll(polylinePoints)
                                    .color(Color.BLUE)
                                    .width(10f)
                                //add to map
                                googleMap?.addPolyline(polylineOptions)

                                // Update camera to show the entire route with a closer zoom
                                val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                                polylinePoints.forEach { boundsBuilder.include(it) }
                                val bounds = boundsBuilder.build()
                                googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
                            } else {
                                Log.w("DashboardActivity", "No polyline points decoded")
                                Toast.makeText(this@DashboardActivity, "No route points available", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("DashboardActivity", "Error decoding polyline: ${e.message}", e)
                            Toast.makeText(this@DashboardActivity, "Error drawing route: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w("DashboardActivity", "No route found: ${directionsResponse?.status}")
                        Toast.makeText(this@DashboardActivity, "No route found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("DashboardActivity", "Directions API error: ${response.code()}")
                    Toast.makeText(this@DashboardActivity, "Directions API error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DashboardActivity", "Directions fetch failed: ${t.message}", t)
                Toast.makeText(this@DashboardActivity, "Directions fetch failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun fetchBlackspots() {
        if (!isMapReady || googleMap == null) {
            Log.w("DashboardActivity", "Cannot fetch blackspots: Map not ready")
            return
        }

        db.collection("blackspots")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val lat = document.getDouble("latitude") ?: continue
                    val lng = document.getDouble("longitude") ?: continue
                    val description = document.getString("description") ?: "Unknown blackspot"
                    val blackspotLocation = LatLng(lat, lng)
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(blackspotLocation)
                            .title("Blackspot")
                            .snippet(description)
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardActivity", "Failed to fetch blackspots: ${e.message}", e)
                Toast.makeText(this, "Failed to fetch blackspots: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        isMapReady = true
        Log.d("DashboardActivity", "Map is ready")

        // Update map with current location if available
        currentLocation?.let { currentLatLng ->
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            fetchBlackspots()  // Fetch blackspots again if map is ready
        }

        // Process any pending route request
        pendingRouteRequest?.let { (start, end) ->
            Log.d("DashboardActivity", "Processing pending route request: start=$start, end=$end")
            fetchRoute(start, end)
            pendingRouteRequest = null
        }
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