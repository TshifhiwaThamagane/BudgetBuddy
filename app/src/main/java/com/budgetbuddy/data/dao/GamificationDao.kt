package com.budgetbuddy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetbuddy.data.entity.UserGamification

@Dao
interface GamificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(gamification: UserGamification)

    @Query("SELECT * FROM user_gamification WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Int): UserGamification?

    @Query("UPDATE user_gamification SET xp = :xp, level = :level WHERE userId = :userId")
    suspend fun updateXpAndLevel(userId: Int, xp: Int, level: Int)
}
