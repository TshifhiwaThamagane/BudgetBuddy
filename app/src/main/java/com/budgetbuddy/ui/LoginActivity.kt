package com.budgetbuddy.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.util.SessionManager
import kotlinx.coroutines.launch

/**
 * LoginActivity — the app entry point for returning users.
 *
 * Responsibilities:
 *  - Check if the user is already logged in via SessionManager; skip to Dashboard if so.
 *  - Validate the email and password fields before attempting a DB lookup.
 *  - Call BudgetRepository.login() on a coroutine, then persist the session on success.
 *
 * References:
 *  - Android Coroutines: https://developer.android.com/kotlin/coroutines
 *  - Patterns.EMAIL_ADDRESS: https://developer.android.com/reference/android/util/Patterns
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        // TAG is used to filter Logcat output for this screen
        private const val TAG = "LoginActivity"
    }

    private lateinit var repository: BudgetRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d(TAG, "onCreate: LoginActivity started")

        repository = BudgetRepository.getInstance(this)
        sessionManager = SessionManager(this)

        // If a valid session already exists, skip login and go straight to the dashboard
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "User already logged in (id=${sessionManager.getUserId()}), redirecting to Dashboard")
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Basic input validation before hitting the database
            if (email.isEmpty() || password.isEmpty()) {
                Log.d(TAG, "Login attempt blocked: empty email or password")
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Log.d(TAG, "Login attempt blocked: invalid email format -> $email")
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Attempting login for email: $email")

            // Launch a coroutine tied to this Activity's lifecycle to query the DB
            lifecycleScope.launch {
                val user = repository.login(email, password)

                if (user != null) {
                    // Persist user session so the app remembers who is logged in
                    Log.d(TAG, "Login successful for userId=${user.id}, name=${user.fullName}")
                    sessionManager.saveUser(user.id, user.fullName)
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    // Credentials did not match any record in the database
                    Log.e(TAG, "Login failed: no matching user found for email=$email")
                    Toast.makeText(this@LoginActivity, "Invalid login details", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Navigate to registration screen
        tvRegister.setOnClickListener {
            Log.d(TAG, "Navigating to RegisterActivity")
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Navigate to password reset screen
        tvForgotPassword.setOnClickListener {
            Log.d(TAG, "Navigating to ForgotPasswordActivity")
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}
