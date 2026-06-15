package com.budgetbuddy.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_gamification")
data class UserGamification(
    @PrimaryKey val userId: Int,
    val xp: Int = 0,
    val level: Int = 1
)
