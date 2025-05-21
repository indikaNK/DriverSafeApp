package com.example.driversafeapp_application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            // Locate UI elements
            val emailField = findViewById<EditText>(R.id.emailField)
            val passwordField = findViewById<EditText>(R.id.passwordField)
            val loginButton = findViewById<Button>(R.id.loginButton)

            // Check if views are found
            if (emailField == null || passwordField == null || loginButton == null) {
                Log.e("MainActivity", "One or more UI elements not found")
                Toast.makeText(this, "UI setup error", Toast.LENGTH_LONG).show()
                return
            }

            // Set a listener for login button
            loginButton.setOnClickListener {
                val email = emailField.text.toString().trim()
                val password = passwordField.text.toString().trim()

                // Empty check
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                } else {
                    // Authentication logic
                    if (email == "test@gmail.com" && password == "password123") {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        // Start DashboardActivity
                         val intent = Intent(this, DashboardActivity::class.java)
                         startActivity(intent)
                    } else {
                        Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Crash in onCreate: ${e.message}", e)
            Toast.makeText(this, "App crashed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }// end on create

    //





}// end main activity