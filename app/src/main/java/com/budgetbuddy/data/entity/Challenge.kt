package com.budgetbuddy.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val challengeType: String,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currentProgress: Double = 0.0,
    val startDate: String,
    val endDate: String,
    val isCompleted: Boolean = false,
    val rewardPoints: Int = 50
)
