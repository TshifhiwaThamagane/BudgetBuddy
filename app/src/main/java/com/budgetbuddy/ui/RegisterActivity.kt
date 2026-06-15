package com.budgetbuddy.ui

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import kotlinx.coroutines.launch

/**
 * RegisterActivity — allows a new user to create a BudgetBuddy account.
 *
 * Validation rules enforced before the DB call:
 *  - All four fields (name, email, password, confirm password) must be non-empty.
 *  - Email must match Android's built-in EMAIL_ADDRESS pattern.
 *  - Password must be at least 6 characters.
 *  - Password and confirm-password fields must match.
 *
 * On success the Activity finishes and returns to LoginActivity.
 *
 * References:
 *  - Room insert via repository: BudgetRepository.registerUser()
 *  - Coroutines + lifecycleScope: https://developer.android.com/kotlin/coroutines
 */
class RegisterActivity : AppCompatActivity() {

    companion object {
        // TAG used to filter Logcat entries for this screen
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        Log.d(TAG, "onCreate: RegisterActivity started")

        val repository = BudgetRepository.getInstance(this)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // --- Input validation ---

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Log.d(TAG, "Registration blocked: one or more required fields are empty")
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Log.d(TAG, "Registration blocked: invalid email format -> $email")
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Log.d(TAG, "Registration blocked: password too short (${password.length} chars)")
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Log.d(TAG, "Registration blocked: passwords do not match")
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Validation passed. Attempting to register user: $email")

            // Launch coroutine to perform the DB insert off the main thread
            lifecycleScope.launch {
                val result = repository.registerUser(name, email, password)

                if (result.isSuccess) {
                    Log.d(TAG, "Registration successful for email: $email")
                    Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                    finish() // Return to LoginActivity
                } else {
                    // The repository returns a failure if the email already exists
                    val errorMsg = result.exceptionOrNull()?.message ?: "Registration failed"
                    Log.e(TAG, "Registration failed: $errorMsg")
                    Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
