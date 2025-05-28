package com.example.driversafeapp_application

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class SettingsActivity : AppCompatActivity() {
    //non null property :: user preferred value store
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize SharedPreferences
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("DriverSafePrefs", MODE_PRIVATE)


        // back button
        val backButton = findViewById<Button>(R.id.backButton)

        //set on click listener
        backButton.setOnClickListener {
            //navigates to home
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            //close activity
            finish()
        }

        // Notifications Switch
        val notificationsSwitch = findViewById<Switch>(R.id.notificationsSwitch)
        notificationsSwitch.isChecked = sharedPreferences.getBoolean("notifications", true)
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Notifications Enabled" else "Notifications Disabled", Toast.LENGTH_SHORT).show()
        }

        // GPS Location Data Access Switch
        val gpsSwitch = findViewById<Switch>(R.id.gpsSwitch)
        gpsSwitch.isChecked = sharedPreferences.getBoolean("gps", true)
        gpsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("gps", isChecked).apply()
            Toast.makeText(this, if (isChecked) "GPS Access Enabled" else "GPS Access Disabled", Toast.LENGTH_SHORT).show()
        }

        // logout the user
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        //set on click listener
        logoutButton.setOnClickListener {
            // Sign out from Firebase
            auth.signOut()
            // optional : clear all shared preferences
            sharedPreferences.edit().clear().apply();

            Toast.makeText(this, "See you soon!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)

            //clear activity stack dashboard activity and start activity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()

        }
    }



}