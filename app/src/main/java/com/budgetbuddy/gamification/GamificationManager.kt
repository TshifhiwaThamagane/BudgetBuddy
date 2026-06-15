package com.budgetbuddy.gamification

import android.util.Log
import com.budgetbuddy.data.dao.AchievementDao
import com.budgetbuddy.data.dao.BadgeDao
import com.budgetbuddy.data.dao.ExpenseDao
import com.budgetbuddy.data.dao.GamificationDao
import com.budgetbuddy.data.dao.StreakDao
import com.budgetbuddy.data.entity.Achievement
import com.budgetbuddy.data.entity.UserBadge
import com.budgetbuddy.data.entity.UserGamification
import com.budgetbuddy.data.entity.UserStreak
import com.budgetbuddy.util.BudgetCalculator
import com.budgetbuddy.util.DateUtils

/**
 * Manages XP, levels, badge unlocking, and achievement history.
 */
class GamificationManager(
    private val gamificationDao: GamificationDao,
    private val badgeDao: BadgeDao,
    private val achievementDao: AchievementDao,
    private val streakDao: StreakDao,
    private val expenseDao: ExpenseDao
) {
    companion object {
        private const val TAG = "GamificationManager"
        private const val EXPENSE_EXPLORER_COUNT = 10
        private const val CATEGORY_CHAMPION_COUNT = 3
    }

    data class UnlockResult(
        val newBadges: List<Badge> = emptyList(),
        val totalXpEarned: Int = 0
    )

    suspend fun ensureUserProfile(userId: Int) {
        if (gamificationDao.getByUserId(userId) == null) {
            gamificationDao.upsert(UserGamification(userId = userId))
        }
        if (streakDao.getByUserId(userId) == null) {
            streakDao.upsert(UserStreak(userId = userId))
        }
    }

    suspend fun getGamification(userId: Int): UserGamification {
        ensureUserProfile(userId)
        return gamificationDao.getByUserId(userId)!!
    }

    suspend fun getStreak(userId: Int): UserStreak {
        ensureUserProfile(userId)
        return streakDao.getByUserId(userId)!!
    }

    suspend fun getUnlockedBadges(userId: Int): List<UserBadge> = badgeDao.getByUserId(userId)

    suspend fun getAchievements(userId: Int) = achievementDao.getByUserId(userId)

    suspend fun onExpenseLogged(userId: Int, expenseDate: String): UnlockResult {
        Log.d(TAG, "Processing gamification for user $userId on date $expenseDate")
        ensureUserProfile(userId)
        updateStreak(userId, expenseDate)

        val newBadges = mutableListOf<Badge>()
        var totalXp = 0

        val expenseCount = expenseDao.getExpenseCount(userId)
        if (expenseCount == 1) {
            unlockBadge(userId, Badge.FIRST_EXPENSE)?.let {
                newBadges.add(it)
                totalXp += it.xpReward
            }
        }
        if (expenseCount >= EXPENSE_EXPLORER_COUNT) {
            unlockBadge(userId, Badge.EXPENSE_EXPLORER)?.let {
                newBadges.add(it)
                totalXp += it.xpReward
            }
        }

        val streak = streakDao.getByUserId(userId)!!
        if (streak.dailyStreak >= 7) {
            unlockBadge(userId, Badge.SEVEN_DAY_STREAK)?.let {
                newBadges.add(it)
                totalXp += it.xpReward
            }
        }
        if (streak.dailyStreak >= 30) {
            unlockBadge(userId, Badge.THIRTY_DAY_STREAK)?.let {
                newBadges.add(it)
                totalXp += it.xpReward
            }
        }

        val categoryCount = expenseDao.getDistinctCategoryCount(userId)
        if (categoryCount >= CATEGORY_CHAMPION_COUNT) {
            unlockBadge(userId, Badge.CATEGORY_CHAMPION)?.let {
                newBadges.add(it)
                totalXp += it.xpReward
            }
        }

        if (totalXp > 0) {
            addXp(userId, totalXp)
        }

        return UnlockResult(newBadges, totalXp)
    }

    suspend fun checkBudgetBadges(userId: Int, monthlySpent: Double, monthlyBudget: Double) {
        if (monthlySpent <= monthlyBudget) {
            unlockBadge(userId, Badge.BUDGET_MASTER)?.let { addXp(userId, it.xpReward) }
        }
        val savings = monthlyBudget - monthlySpent
        if (savings >= monthlyBudget * 0.2) {
            unlockBadge(userId, Badge.SAVINGS_HERO)?.let { addXp(userId, it.xpReward) }
        }
    }

    private suspend fun updateStreak(userId: Int, expenseDate: String) {
        val current = streakDao.getByUserId(userId) ?: UserStreak(userId = userId)
        val lastDate = current.lastLogDate

        val newDaily = when {
            DateUtils.isSameDay(lastDate, expenseDate) -> current.dailyStreak
            DateUtils.isConsecutiveDay(lastDate, expenseDate) -> current.dailyStreak + 1
            else -> 1
        }

        val newWeekly = if (newDaily >= 7) current.weeklyStreak + 1 else current.weeklyStreak
        val newBest = maxOf(current.bestStreak, newDaily)

        streakDao.upsert(
            current.copy(
                dailyStreak = newDaily,
                weeklyStreak = newWeekly,
                bestStreak = newBest,
                lastLogDate = expenseDate
            )
        )
        Log.d(TAG, "Streak updated: daily=$newDaily, weekly=$newWeekly, best=$newBest")
    }

    private suspend fun unlockBadge(userId: Int, badgeId: String): Badge? {
        if (badgeDao.hasBadge(userId, badgeId) > 0) return null
        val badge = Badge.findById(badgeId) ?: return null

        badgeDao.insert(UserBadge(userId = userId, badgeId = badgeId))
        achievementDao.insert(
            Achievement(
                userId = userId,
                title = badge.name,
                description = badge.description,
                xpEarned = badge.xpReward
            )
        )
        Log.d(TAG, "Badge unlocked: ${badge.name}")
        return badge
    }

    private suspend fun addXp(userId: Int, xpToAdd: Int) {
        val current = gamificationDao.getByUserId(userId) ?: UserGamification(userId = userId)
        val newXp = current.xp + xpToAdd
        val newLevel = BudgetCalculator.levelFromXp(newXp)
        gamificationDao.updateXpAndLevel(userId, newXp, newLevel)
        Log.d(TAG, "XP added: +$xpToAdd, total=$newXp, level=$newLevel")
    }
}
