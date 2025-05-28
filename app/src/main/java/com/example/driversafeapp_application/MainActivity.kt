package com.example.driversafeapp_application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            Log.d("MainActivity", "Firebase initialized successfully")

            // Initialize Firebase Auth and Firestore
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()

            // Check if user is already logged in
            Log.d("MainActivity", "Checking if user is logged in: ${auth.currentUser?.email}")
            if (auth.currentUser != null) {

                //Test::write a demo object to see if the connection works
//                writeDemoObjectToFirestore()

                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
                return
            }else {
                Log.d("MainActivity", "No user logged in, staying on login screen")
            }


            // Locate UI elements
            val emailField = findViewById<EditText>(R.id.emailField)
            val passwordField = findViewById<EditText>(R.id.passwordField)
            val loginButton = findViewById<Button>(R.id.loginButton)
            val registerButton = findViewById<Button>(R.id.registerButton)


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
                    return@setOnClickListener

                }

//                 Firebase sign in using email and password
                auth.signInWithEmailAndPassword(email,password)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
//                        writeDemoObjectToFirestore()

                        //navigate to dashboard
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()

                    }



                // enable only for test
//                else {
//                    // Authentication logic
//                    if (email == "test@gmail.com" && password == "password123") {
//                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
//                        // Start DashboardActivity
//                         val intent = Intent(this, DashboardActivity::class.java)
//                         startActivity(intent)
//                    } else {
//                        Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
//                    }
//                }






            }

            // Go to register page

            registerButton.setOnClickListener {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
                finish()
            }


        } catch (e: Exception) {
            Log.e("MainActivity", "Crash in onCreate: ${e.message}", e)
            Toast.makeText(this, "App crashed: ${e.message}", Toast.LENGTH_LONG).show()
        }








    }// end on create

// Test: Enable to see if write works to firestore
//    private fun writeDemoObjectToFirestore() {
//        // Ensure user is authenticated
//        val user = auth.currentUser
//        if (user == null) {
//
//            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // Create a demo object
//        val demoObject = hashMapOf(
//            "userId" to user.uid,
//            "message" to "Hello from DriverSafe!",
//            "timestamp" to System.currentTimeMillis()
//        )
//
//        // Write to Firestore
//        db.collection("demo").document()
//            .set(demoObject)
//            .addOnSuccessListener {
//                Toast.makeText(this, "Successfully wrote to Firestore!", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Failed to write to Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }

    //





}// end main activity