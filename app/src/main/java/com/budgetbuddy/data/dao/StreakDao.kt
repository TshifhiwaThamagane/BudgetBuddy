package com.budgetbuddy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetbuddy.data.entity.UserStreak

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: UserStreak)

    @Query("SELECT * FROM user_streaks WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Int): UserStreak?
}
