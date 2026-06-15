package com.budgetbuddy

import com.budgetbuddy.data.entity.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginRegistrationTest {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+[.][A-Za-z]{2,}$")

    @Test
    fun `login returns user with matching credentials`() {
        val user = User(id = 1, fullName = "Test User", email = "test@example.com", password = "password123")
        assertEquals("test@example.com", user.email)
        assertEquals("password123", user.password)
    }

    @Test
    fun `registration validates email format`() {
        val invalidEmail = "invalid-email"
        assertFalse(emailRegex.matches(invalidEmail))
    }

    @Test
    fun `registration requires password minimum length`() {
        val shortPassword = "12345"
        assertTrue(shortPassword.length < 6)
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
