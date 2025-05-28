package com.example.driversafeapp_application

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText

class RouteInputActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_route_input)

        // Locate UI elements
        val destinationField = findViewById<TextInputEditText>(R.id.destinationField)
        val startJourneyButton = findViewById<Button>(R.id.startJourneyButton)

        startJourneyButton.setOnClickListener {
            val destination = destinationField.text.toString().trim()

            if (destination.isEmpty()){
                Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Return the destination to DashboardActivity
            val resultIntent = Intent()
            resultIntent.putExtra("destination", destination)
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        }
    }



