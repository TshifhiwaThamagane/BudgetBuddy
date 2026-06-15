package com.budgetbuddy.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_streaks")
data class UserStreak(
    @PrimaryKey val userId: Int,
    val dailyStreak: Int = 0,
    val weeklyStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastLogDate: String? = null
)
