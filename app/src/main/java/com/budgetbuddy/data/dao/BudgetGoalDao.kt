package com.budgetbuddy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetbuddy.data.entity.BudgetGoal

@Dao
interface BudgetGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: BudgetGoal)

    @Query("SELECT * FROM budget_goals WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Int): BudgetGoal?
}
