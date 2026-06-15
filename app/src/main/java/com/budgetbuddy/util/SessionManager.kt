package com.budgetbuddy.util

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("budget_session", Context.MODE_PRIVATE)

    fun saveUser(userId: Int, name: String) {
        prefs.edit()
            .putInt("user_id", userId)
            .putString("user_name", name)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun getUserId(): Int = prefs.getInt("user_id", -1)

    fun getUserName(): String = prefs.getString("user_name", "User") ?: "User"

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false) && getUserId() != -1

    fun logout() {
        prefs.edit().clear().apply()
    }
}
