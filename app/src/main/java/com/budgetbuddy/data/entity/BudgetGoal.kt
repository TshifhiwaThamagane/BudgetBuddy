package com.budgetbuddy.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey val userId: Int,
    val monthlyBudget: Double = 5000.0,
    val minGoal: Double = 2000.0,
    val maxGoal: Double = 4500.0
)
