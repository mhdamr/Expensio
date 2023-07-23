package com.example.expensio.Auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import com.example.expensio.MainActivity
import com.example.expensio.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user is already logged in
        auth = FirebaseAuth.getInstance()

        // Continue with the login flow as usual
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        // Get references to UI elements
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)
        progressBar = findViewById(R.id.progressBar)

        // Get reference to SharedPreferences
        prefs = getSharedPreferences("com.example.expensio", Context.MODE_PRIVATE)

        // Set click listener for login button
        loginButton.setOnClickListener {
            loginUser()
        }

        // Set click listener for register button
        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }


        // Set click listener for forgot password button
        val forgotPasswordText: TextView = findViewById(R.id.textViewForgotPassword)

        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Authenticate the user with Firebase Auth and remember if the user logged in.
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        val hasLoggedInBefore = prefs.getBoolean("hasLoggedInBefore", false)

                        if (!hasLoggedInBefore) {
                            // Save that the user has logged in for the first time
                            prefs.edit().putBoolean("hasLoggedInBefore", true).apply()
                        }

                        // Login successful and email is verified, navigate to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Email not verified, sign the user out and display message
                        auth.signOut()
                        Toast.makeText(baseContext, "Please verify your email before logging in", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Login failed, display error message
                    Toast.makeText(baseContext, "Login failed: Incorrect Credentials", Toast.LENGTH_SHORT).show()
                }

                progressBar.visibility = View.GONE
            }
    }
}