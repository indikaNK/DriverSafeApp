package com.example.driversafeapp_application



//maps google

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.driversafeapp_application.api.DirectionsResponse
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
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import com.example.driversafeapp_application.api.DirectionsApiService
import com.example.driversafeapp_application.api.RetrofitClient
import com.example.driversafeapp_application.api.WeatherApiService

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.Marker
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

import com.example.driversafeapp_application.api.WeatherResponse


class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {


    // get last know location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // google map view
    private lateinit var mapView: MapView
    private  var googleMap: GoogleMap? = null
    private lateinit var locationText: TextView
    private lateinit var speedText: TextView
    private lateinit var usernameText: TextView // Added TextView for username

    private val locationPermissionCode = 100
    private var currentLocation: LatLng? = null
    private var destinationLocation: LatLng? = null
    private var isMapReady = false
    private var pendingRouteRequest: Pair<LatLng, LatLng>? = null
    private var isLocationReady = false
    private val mawanellaLocation = LatLng(7.252431177548869, 80.44684581600977) // Mawanella as destination 7.252431177548869, 80.44684581600977
    private var isRouteDisplayed = false // Flag to track if a route is currently displayed



    private lateinit var pickRouteButton: Button
    private lateinit var simulateJourneyButton: Button


    //weather view
    private lateinit var weatherText: TextView
    private lateinit var weatherApiService: WeatherApiService
    private val openWeatherApiKey = "a95567bb398bc0266cd2b9e0d0049cae"
    private val googleMapsApiKey = "AIzaSyB-u93Huo2uyhIVLPsyNxNW7hg5EMb7CsM"

    //direction
    private lateinit var directionsApiService: DirectionsApiService

    //firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Better alerts Blackspot Proximity
    private lateinit var locationCallback: LocationCallback
    private var blackspots: List<Blackspot> = emptyList()
    private var lastAlertedBlackspot: String? = null // To prevent repeated alerts for the same blackspot
    private var alertDialog: AlertDialog? = null // To manage the current alert dialog
    private val ALERT_DISTANCE_THRESHOLD = 100.0 // Distance threshold in meters (configurable)
    private val VIBRATION_DURATION = 500L // Vibration duration in milliseconds
    private lateinit var vibrator: Vibrator
    private lateinit var mediaPlayer: MediaPlayer

    private var polylinePoints: List<LatLng> = emptyList() // Store polyline points for simulation
    private var isSimulating = false // Track if simulation is in progress
    private var simulationHandler: Handler = Handler(Looper.getMainLooper())
    private var simulationRunnable: Runnable? = null
    private var currentLocationMarker: Marker? = null // Marker for current location
    private var markerIconBitmap: Bitmap? = null // Store the marker icon Bitmap to prevent garbage collection

    data class Blackspot(val id: String, val location: LatLng, val description: String)

    // Utility function to convert a drawable resource to a Bitmap
    private fun drawableToBitmap(context: Context, drawableId: Int): Bitmap? {
        val drawable: Drawable? = ContextCompat.getDrawable(context, drawableId)
        drawable?.let {
            val wrappedDrawable = DrawableCompat.wrap(it).mutate()
            val width = wrappedDrawable.intrinsicWidth
            val height = wrappedDrawable.intrinsicHeight
            if (width <= 0 || height <= 0) {
                Log.e("DashboardActivity", "Drawable has invalid dimensions: width=$width, height=$height")
                return null
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            wrappedDrawable.setBounds(0, 0, canvas.width, canvas.height)
            wrappedDrawable.draw(canvas)
            return bitmap
        }
        Log.e("DashboardActivity", "Failed to load drawable resource: $drawableId")
        return null
    }

    private val routeInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        Log.d("DashboardActivity", "Received result from RouteInputActivity: resultCode=${result.resultCode}")

        if (result.resultCode == RESULT_OK) {

            val lat = result.data?.getDoubleExtra("destinationLat", -1.0) ?: -1.0
            val lng = result.data?.getDoubleExtra("destinationLng", -1.0) ?: -1.0

            if (lat == -1.0 || lng == -1.0) {
                Log.e("DashboardActivity", "Invalid coordinates received: lat=$lat, lng=$lng")
                Toast.makeText(this, "Could not determine destination coordinates", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

//            val destination = result.data?.getStringExtra("destination")
            destinationLocation = LatLng(lat, lng)

            Log.d("DashboardActivity", "Destination coordinates received: lat/lng: ($lat,$lng)")
            Log.d("DashboardActivity", "Current location state: $currentLocation")

//              Enable for testing only
//            if (destination == null) {
//                Log.e("DashboardActivity", "Destination is null in route input result")
//                Toast.makeText(this, "No destination provided", Toast.LENGTH_SHORT).show()
//                return@registerForActivityResult
//            }
//            Log.d("DashboardActivity", "Destination received: $destination")
//            // For testing, set Mawanella as the destination
//            destinationLocation = mawanellaLocation
//            Log.d("DashboardActivity", "Current location state: $currentLocation")


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


        // Initialize Vibrator and MediaPlayer
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mediaPlayer = MediaPlayer.create(this, R.raw.ding) // Use a default system alert sound
        mediaPlayer.setOnCompletionListener { mp ->
            try {
                mp.stop()
                mp.reset()
                mp.setDataSource(this@DashboardActivity, android.net.Uri.parse("android.resource://${packageName}/${R.raw.ding}"))
                mp.prepare()
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error resetting MediaPlayer after completion: ${e.message}", e)
            }
        }

        // Initialize the marker icon Bitmap
        markerIconBitmap = drawableToBitmap(this, R.drawable.ic_red_arrow)


// Initialize LocationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    val speedKmh = location.speed * 3.6 // Convert m/s to km/h
                    locationText.text = "Location: Lat ${"%.4f".format(location.latitude)}, Lng ${"%.4f".format(location.longitude)}"
                    speedText.text = "Speed: ${"%.1f".format(speedKmh)} km/h"

                    currentLocation = currentLatLng
                    isLocationReady = true
                    Log.d("DashboardActivity", "Fresh location received: $currentLocation, enabling Pick Route button")
                    pickRouteButton.isEnabled = true  // Enable button now that location is ready

                    // Update map with current location if map is ready
                    if (isMapReady && googleMap != null) {
                        if (!isRouteDisplayed) {
                            // Clear the map only if no route is displayed
                            googleMap?.clear()
                            // Redraw blackspots
                            blackspots.forEach { blackspot ->
                                googleMap?.addMarker(
                                    MarkerOptions()
                                        .position(blackspot.location)
                                        .title("Blackspot")
                                        .snippet(blackspot.description)
                                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                                )
                            }
                        }
                        // Ensure the current location marker is always created or updated
                        if (currentLocationMarker == null) {
                            currentLocationMarker = if (markerIconBitmap != null) {
                                googleMap?.addMarker(
                                    MarkerOptions()
                                        .position(currentLatLng)
                                        .title("Current Location")
                                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(markerIconBitmap!!))
                                )
                            } else {
                                Log.w("DashboardActivity", "Failed to load custom icon in onLocationResult, using default marker")
                                googleMap?.addMarker(
                                    MarkerOptions()
                                        .position(currentLatLng)
                                        .title("Current Location")
                                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                                )
                            }
                            Log.d("DashboardActivity", "Created currentLocationMarker in onLocationResult: $currentLocationMarker")
                        } else {
                            currentLocationMarker?.position = currentLatLng
                            // Only re-set the icon if it hasn't been set or if the marker was removed
                            if (currentLocationMarker?.isVisible == false || currentLocationMarker?.tag == null) {
                                if (markerIconBitmap != null) {
                                    currentLocationMarker?.setIcon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(markerIconBitmap!!))
                                    currentLocationMarker?.tag = "set" // Mark as icon set
                                } else {
                                    Log.w("DashboardActivity", "Failed to load custom icon in onLocationResult, using default marker")
                                    currentLocationMarker?.setIcon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                                    currentLocationMarker?.tag = "set"
                                }
                            }
                            Log.d("DashboardActivity", "Updated currentLocationMarker position in onLocationResult: $currentLatLng, visible: ${currentLocationMarker?.isVisible}")
                        }
                        if (!isSimulating) {
                            // Only move the camera if not simulating (simulation handles camera movement)
                            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        }
                    } else {
                        Log.w("DashboardActivity", "Map not ready when updating current location")
                    }

                    // Fetch weather data
                    fetchWeatherData(location.latitude, location.longitude)

                    // Check proximity to blackspots
                    checkProximityToBlackspots(currentLatLng)
                } ?: run {
                    Log.w("DashboardActivity", "Location update received but location is null")
                    Toast.makeText(this@DashboardActivity, "Unable to get current location, please ensure GPS is enabled", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Initialize Retrofit services
        weatherApiService = RetrofitClient.weatherApiService
        directionsApiService = RetrofitClient.directionsApiService

        // Initialize MapView
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)


        // Initialize TextViews
        locationText = findViewById(R.id.locationText)
        speedText = findViewById(R.id.speedText)
        weatherText = findViewById(R.id.weatherText)
        usernameText = findViewById(R.id.usernameText) // Initialize the username TextView




         // Settings Icon Button
        val settingsIconButton = findViewById<ImageButton>(R.id.settingsIconButton)
        settingsIconButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        pickRouteButton = findViewById<Button>(R.id.pickRouteButton)
        simulateJourneyButton = findViewById<Button>(R.id.simulateJourneyButton)

        pickRouteButton.isEnabled = false  // Disable until location is ready

        pickRouteButton.setOnClickListener {
            Log.d("DashboardActivity", "Pick Route clicked, currentLocation: $currentLocation")
            // Clear the previous route before fetching a new one
            isRouteDisplayed = false // set is route displayed to false
            googleMap?.clear() // clear the current map
            currentLocationMarker = null
            polylinePoints = emptyList()
            routeInputLauncher.launch(Intent(this, RouteInputActivity::class.java))
        }



        // Simulate Journey Button
        simulateJourneyButton.setOnClickListener {
            if (polylinePoints.isNotEmpty() && !isSimulating) {
                startJourneySimulation()
            } else {
                Toast.makeText(this, "No route available to simulate. Please pick a route first.", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch and display the username
        fetchAndDisplayUsername()

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

    // Function to fetch the username from Firestore and display it
    private fun fetchAndDisplayUsername() {
        val user = auth.currentUser
        if (user == null) {
            Log.w("DashboardActivity", "No user is currently signed in")
            usernameText.text = "Welcome, Guest"
            Toast.makeText(this, "Please sign in to continue", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = user.uid
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    if (username != null) {
                        usernameText.text = "Welcome To Drivesafe, $username"
                        Log.d("DashboardActivity", "Successfully fetched username: $username")
                    } else {
                        Log.w("DashboardActivity", "Username field is missing in Firestore document for user: $userId")
                        usernameText.text = "Welcome, User"
                    }
                } else {
                    Log.w("DashboardActivity", "No Firestore document found for user: $userId")
                    usernameText.text = "Welcome, User"
                }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardActivity", "Failed to fetch username from Firestore: ${e.message}", e)
                usernameText.text = "Welcome, User"
                Toast.makeText(this, "Failed to fetch username: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("DashboardActivity", "Permissions not granted in getCurrentLocation")
            return
        }
        // Create a LocationRequest for continuous updates
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // Update every 10 seconds
            fastestInterval = 2000 // Fastest update interval
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        Log.d("DashboardActivity", "Requested continuous location updates")
    }

    // Calculate distance between two LatLng points using Haversine formula (in meters)
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLng = Math.toRadians(point2.longitude - point1.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(point1.latitude)) * cos(Math.toRadians(point2.latitude)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun checkProximityToBlackspots(userLocation: LatLng) {
        blackspots.forEach { blackspot ->
            val distance = calculateDistance(userLocation, blackspot.location)
            if (distance <= ALERT_DISTANCE_THRESHOLD && lastAlertedBlackspot != blackspot.id) {
                // Dismiss any existing dialog
                alertDialog?.dismiss()

                // Show alert dialog for nearby blackspot
                val message = "You are near a blackspot: ${blackspot.description}\nDistance: ${"%.1f".format(distance)} meters"
                alertDialog = AlertDialog.Builder(this)
                    .setTitle("Blackspot Warning")
                    .setMessage(message)
                    .setPositiveButton("Dismiss") { dialog, _ ->
                        lastAlertedBlackspot = blackspot.id
                        dialog.dismiss()
                    }
                    .setCancelable(false) // Prevent dismissal by tapping outside
                    .create()
                alertDialog?.show()

                // Play alert sound
                try {
                    mediaPlayer.start()
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error playing alert sound: ${e.message}", e)
                }

                // Trigger vibration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(VIBRATION_DURATION)
                }

                Log.d("DashboardActivity", "Blackspot alert: ${blackspot.description} at distance $distance meters")
            } else if (distance > ALERT_DISTANCE_THRESHOLD && lastAlertedBlackspot == blackspot.id) {
                // Reset alert if user moves away from the blackspot
                lastAlertedBlackspot = null
                alertDialog?.dismiss()
            }
        }
    }


    private fun startJourneySimulation() {
        isSimulating = true
        simulateJourneyButton.isEnabled = false // Disable button during simulation
        pickRouteButton.isEnabled = false // Disable route picking during simulation

        // Stop real location updates to avoid interference
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Log initial state
        Log.d("DashboardActivity", "Starting simulation: isMapReady=$isMapReady, googleMap=$googleMap, currentLocation=$currentLocation, currentLocationMarker=$currentLocationMarker")

        // Draw the polyline and blackspot markers once at the start of the simulation
        if (isMapReady && googleMap != null) {
//            googleMap?.clear() // Clear the map to start fresh // removed because it causes the dissapear in current marker

            // Draw the polyline
            val polylineOptions = PolylineOptions()
                .addAll(polylinePoints)
                .color(Color.BLUE)
                .width(10f)
            googleMap?.addPolyline(polylineOptions)

            // Draw blackspot markers
            blackspots.forEach { blackspot ->
                googleMap?.addMarker(
                    MarkerOptions()
                        .position(blackspot.location)
                        .title("Blackspot")
                        .snippet(blackspot.description)
                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                )
            }

            // Ensure currentLocation is set to the first point of the polyline if null
            if (currentLocation == null && polylinePoints.isNotEmpty()) {
                currentLocation = polylinePoints[0]
                Log.d("DashboardActivity", "Initialized currentLocation to first polyline point: $currentLocation")
            }

            // Initialize the current location marker if not already set
            if (currentLocationMarker == null && currentLocation != null) {
                currentLocationMarker = if (markerIconBitmap != null) {
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(currentLocation!!)
                            .title("Current Location")
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(markerIconBitmap!!))
                    )
                } else {
                    // Fallback to default marker if the bitmap conversion fails
                    Log.w("DashboardActivity", "Failed to load custom icon in startJourneySimulation, using default marker")
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(currentLocation!!)
                            .title("Current Location")
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                    )
                }
                Log.d("DashboardActivity", "Initialized currentLocationMarker in startJourneySimulation at position: $currentLocation, marker: $currentLocationMarker")
            } else if (currentLocationMarker == null) {
                Log.w("DashboardActivity", "Cannot initialize currentLocationMarker in startJourneySimulation: currentLocation is null")
            }
        } else {
            Log.w("DashboardActivity", "Cannot start simulation: Map not ready (isMapReady=$isMapReady, googleMap=$googleMap)")
            Toast.makeText(this, "Map not ready, please try again", Toast.LENGTH_SHORT).show()
            isSimulating = false
            simulateJourneyButton.isEnabled = true
            pickRouteButton.isEnabled = true
            return
        }

        // Simulate movement along the polyline
        var currentIndex = 0
        val simulationSpeed = 5 // Move 5 points per second (adjust for speed)
        val simulationInterval = 1000L // Update every 1 second

        simulationRunnable = object : Runnable {
            override fun run() {
                if (currentIndex < polylinePoints.size) {
                    currentLocation = polylinePoints[currentIndex]
                    locationText.text = "Location: Lat ${"%.4f".format(currentLocation!!.latitude)}, Lng ${"%.4f".format(currentLocation!!.longitude)}"
                    speedText.text = "Speed: Simulated"

                    // Update marker position on the map
                    if (isMapReady && googleMap != null) {
                        // Update the current location marker's position
                        currentLocationMarker?.position = currentLocation!!
                        Log.d("DashboardActivity", "Updated currentLocationMarker position in simulation to: $currentLocation, visible: ${currentLocationMarker?.isVisible}")
                        // Smoothly animate the camera to follow the marker
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f), 1000, null)
                    } else {
                        Log.w("DashboardActivity", "Cannot update marker: Map not ready during simulation")
                    }

                    // Check proximity to blackspots
                    checkProximityToBlackspots(currentLocation!!)

                    currentIndex += simulationSpeed
                    simulationHandler.postDelayed(this, simulationInterval)
                } else {
                    // Simulation complete
                    isSimulating = false
                    simulateJourneyButton.isEnabled = true
                    pickRouteButton.isEnabled = true
                    Toast.makeText(this@DashboardActivity, "Simulation complete", Toast.LENGTH_SHORT).show()
                    // Resume real location updates
                    getCurrentLocation()
                }
            }
        }
        simulationHandler.post(simulationRunnable!!)
    }


    // fetch weather from API
    private fun fetchWeatherData(latitude: Double, longitude: Double) {

        Log.d("DashboardActivity", "Fetching weather data for lat=$latitude, lon=$longitude")
        val call = weatherApiService.getCurrentWeather(latitude, longitude, "metric", openWeatherApiKey)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    if (weatherResponse != null) {
                        Log.d("DashboardActivity", "Weather API response: $weatherResponse")
                        if (weatherResponse.cod == 200) {
                            weatherText.text = "Weather: ${weatherResponse.weather[0].description.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
                        } else {
                            Log.w("DashboardActivity", "Weather API response code: ${weatherResponse.cod}")
                            weatherText.text = "Weather: Unable to fetch (Code: ${weatherResponse.cod})"
                        }
                    } else {
                        Log.w("DashboardActivity", "Weather response body is null")
                        weatherText.text = "Weather: Unable to fetch"
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("DashboardActivity", "Weather API error: ${response.code()}, errorBody=$errorBody")
                    Log.e("DashboardActivity", "Weather API raw response: ${response.raw()}")
                    weatherText.text = "Weather: Error fetching data"
                    Toast.makeText(this@DashboardActivity, "Weather API error: ${response.code()}. Error: $errorBody", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("DashboardActivity", "Weather fetch failed: ${t.message}", t)
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
                    if (directionsResponse != null) {
                        Log.d("DashboardActivity", "Directions API response status: ${directionsResponse.status}")
                        Log.d("DashboardActivity", "Directions API response routes: ${directionsResponse.routes}")
                        if (directionsResponse.status == "OK" && directionsResponse.routes.isNotEmpty()) {
                            val route = directionsResponse.routes[0]
                            val points = route.overview_polyline.points
                            Log.d("DashboardActivity", "Encoded polyline points: $points")
                            try {
                                polylinePoints = PolyUtil.decode(points ?: throw IllegalArgumentException("Polyline points are null"))
                                Log.d("DashboardActivity", "Decoded polyline points count: ${polylinePoints.size}")
                                if (polylinePoints.isNotEmpty()) {
                                    val polylineOptions = PolylineOptions()
                                        .addAll(polylinePoints)
                                        .color(Color.BLUE)
                                        .width(10f)
                                    googleMap?.addPolyline(polylineOptions)

                                    // Ensure currentLocation is set to the start of the route if null
                                    if (currentLocation == null) {
                                        currentLocation = start
                                        Log.d("DashboardActivity", "Set currentLocation to route start: $currentLocation")
                                    }

                                    // Redraw current location marker and blackspots
                                    if (currentLocationMarker == null && currentLocation != null) {
                                        currentLocationMarker = if (markerIconBitmap != null) {
                                            googleMap?.addMarker(
                                                MarkerOptions()
                                                    .position(currentLocation!!)
                                                    .title("Current Location")
                                                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(markerIconBitmap!!))
                                            )
                                        } else {
                                            Log.w("DashboardActivity", "Failed to load custom icon in fetchRoute, using default marker")
                                            googleMap?.addMarker(
                                                MarkerOptions()
                                                    .position(currentLocation!!)
                                                    .title("Current Location")
                                                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                                            )
                                        }
                                        Log.d("DashboardActivity", "Created currentLocationMarker in fetchRoute: $currentLocationMarker")
                                    } else if (currentLocationMarker != null) {
                                        currentLocationMarker?.position = currentLocation!!
                                        if (markerIconBitmap != null) {
                                            currentLocationMarker?.setIcon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(markerIconBitmap!!))
                                        } else {
                                            Log.w("DashboardActivity", "Failed to load custom icon in fetchRoute, using default marker")
                                            currentLocationMarker?.setIcon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                                        }
                                        Log.d("DashboardActivity", "Updated currentLocationMarker in fetchRoute: $currentLocationMarker")
                                    } else {
                                        Log.w("DashboardActivity", "Cannot create currentLocationMarker in fetchRoute: currentLocation is null")
                                    }
                                    blackspots.forEach { blackspot ->
                                        googleMap?.addMarker(
                                            MarkerOptions()
                                                .position(blackspot.location)
                                                .title("Blackspot")
                                                .snippet(blackspot.description)
                                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                                        )
                                    }

                                    // Update camera to show the entire route with a closer zoom
                                    val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                                    polylinePoints.forEach { boundsBuilder.include(it) }
                                    val bounds = boundsBuilder.build()
                                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))

                                    // Set the route displayed flag
                                    isRouteDisplayed = true

                                    // Enable the Simulate Journey button
                                    simulateJourneyButton.isEnabled = true
                                } else {
                                    Log.w("DashboardActivity", "No polyline points decoded")
                                    Toast.makeText(this@DashboardActivity, "No route points available", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("DashboardActivity", "Error decoding polyline: ${e.message}", e)
                                Toast.makeText(this@DashboardActivity, "Error drawing route: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.w("DashboardActivity", "No route found: status=${directionsResponse.status}")
                            Toast.makeText(this@DashboardActivity, "No route found: ${directionsResponse.status}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w("DashboardActivity", "Directions response is null")
                        Toast.makeText(this@DashboardActivity, "Directions response is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("DashboardActivity", "Directions API error: ${response.code()}, errorBody=$errorBody")
                    Log.e("DashboardActivity", "Directions API raw response: ${response.raw()}")
                    Toast.makeText(this@DashboardActivity, "Directions API error: ${response.code()}. Error: $errorBody", Toast.LENGTH_LONG).show()
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
                val tempBlackspots = mutableListOf<Blackspot>()
                for (document in result) {
                    val lat = document.getDouble("latitude") ?: continue
                    val lng = document.getDouble("longitude") ?: continue
                    val description = document.getString("description") ?: "Unknown blackspot"
                    val blackspotLocation = LatLng(lat, lng)
                    tempBlackspots.add(Blackspot(document.id, blackspotLocation, description))
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(blackspotLocation)
                            .title("Blackspot")
                            .snippet(description)
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                    )
                    Log.d("DashboardActivity", "Added blackspot marker: $description at $blackspotLocation")
                }
                blackspots = tempBlackspots
                // Check proximity to blackspots if current location is available
                currentLocation?.let { checkProximityToBlackspots(it) }
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
            currentLocationMarker = if (markerIconBitmap != null) {
                googleMap?.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title("Current Location")
                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(markerIconBitmap!!))
                )
            } else {
                // Fallback to default marker if the bitmap conversion fails
                Log.w("DashboardActivity", "Failed to load custom icon in onMapReady, using default marker")
                googleMap?.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title("Current Location")
                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                )
            }
            Log.d("DashboardActivity", "Created currentLocationMarker in onMapReady: $currentLocationMarker")
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        }

        // Process any pending route request
        pendingRouteRequest?.let { (start, end) ->
            Log.d("DashboardActivity", "Processing pending route request: start=$start, end=$end")
            fetchRoute(start, end)
            pendingRouteRequest = null
        }

        // Fetch blackspots after map is ready
        fetchBlackspots()
    }



    // MapView lifecycle methods
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    override fun onPause() {
        super.onPause()
        mapView.onPause()
        // Stop location updates when activity is paused
        fusedLocationClient.removeLocationUpdates(locationCallback)
        // Dismiss any active alert dialog
        alertDialog?.dismiss()
        // Stop simulation if in progress
        simulationRunnable?.let { simulationHandler.removeCallbacks(it) }
        isSimulating = false
        simulateJourneyButton.isEnabled = polylinePoints.isNotEmpty()
        pickRouteButton.isEnabled = true
        // Resume real location updates
        getCurrentLocation()
        // Stop media player if playing
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        // Release MediaPlayer resources
        try {
            mediaPlayer.release()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error releasing MediaPlayer in onDestroy: ${e.message}", e)
        }
        // Clean up the marker icon Bitmap
        markerIconBitmap = null
    }


    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}