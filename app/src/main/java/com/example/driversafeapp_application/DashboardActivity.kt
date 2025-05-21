package com.example.driversafeapp_application



import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val startJourneyButton = findViewById<Button>(R.id.startJourneyButton)
        startJourneyButton.setOnClickListener {
            Toast.makeText(this, "Journey started!", Toast.LENGTH_SHORT).show()
            // Add logic for starting the app's main functionality :: start monitoring
        }
    }
}