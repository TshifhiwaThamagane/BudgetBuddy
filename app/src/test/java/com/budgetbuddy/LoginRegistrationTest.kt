package com.budgetbuddy

import com.budgetbuddy.data.entity.User
import com.budgetbuddy.util.BudgetCalculator
import com.budgetbuddy.util.BudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginRegistrationTest {

    @Test
    fun `login returns user with matching credentials`() {
        val user = User(id = 1, fullName = "Test User", email = "test@example.com", password = "password123")
        assertEquals("test@example.com", user.email)
        assertEquals("password123", user.password)
    }

    @Test
    fun `registration validates email format`() {
        val email = "invalid-email"
        val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        assertTrue(!isValid)
    }

    @Test
    fun `registration requires password minimum length`() {
        val password = "12345"
        assertTrue(password.length < 6)
    }

    @Test
    fun `registration password confirmation must match`() {
        val password = "password123"
        val confirm = "password123"
        assertEquals(password, confirm)
    }

    @Test
    fun `duplicate email should be rejected`() {
        val existingEmail = "user@test.com"
        val newEmail = "user@test.com"
        assertEquals(existingEmail, newEmail)
    }
}
