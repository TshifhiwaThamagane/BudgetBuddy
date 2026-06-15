package com.budgetbuddy.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_badges",
    indices = [androidx.room.Index(value = ["userId", "badgeId"], unique = true)]
)
data class UserBadge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val badgeId: String,
    val unlockedAt: Long = System.currentTimeMillis()
)
