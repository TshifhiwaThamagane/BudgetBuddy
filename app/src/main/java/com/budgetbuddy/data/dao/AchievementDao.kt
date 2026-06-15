package com.budgetbuddy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.budgetbuddy.data.entity.Achievement

@Dao
interface AchievementDao {
    @Insert
    suspend fun insert(achievement: Achievement): Long

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY achievedAt DESC")
    suspend fun getByUserId(userId: Int): List<Achievement>
}
