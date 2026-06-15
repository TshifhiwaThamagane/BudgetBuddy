package com.budgetbuddy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetbuddy.data.entity.UserBadge

@Dao
interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(badge: UserBadge): Long

    @Query("SELECT * FROM user_badges WHERE userId = :userId ORDER BY unlockedAt DESC")
    suspend fun getByUserId(userId: Int): List<UserBadge>

    @Query("SELECT COUNT(*) FROM user_badges WHERE userId = :userId AND badgeId = :badgeId")
    suspend fun hasBadge(userId: Int, badgeId: String): Int
}
