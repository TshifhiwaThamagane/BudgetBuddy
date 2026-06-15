package com.budgetbuddy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetbuddy.data.entity.Challenge

@Dao
interface ChallengeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: Challenge): Long

    @Update
    suspend fun update(challenge: Challenge)

    @Query("SELECT * FROM challenges WHERE userId = :userId AND isCompleted = 0 ORDER BY id DESC")
    suspend fun getActiveChallenges(userId: Int): List<Challenge>

    @Query("SELECT * FROM challenges WHERE userId = :userId AND isCompleted = 1 ORDER BY id DESC")
    suspend fun getCompletedChallenges(userId: Int): List<Challenge>

    @Query("SELECT * FROM challenges WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Challenge?

    @Query(
        """
        SELECT COUNT(*) FROM challenges
        WHERE userId = :userId AND challengeType = :type AND isCompleted = 0
        """
    )
    suspend fun countActiveByType(userId: Int, type: String): Int
}
