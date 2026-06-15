package com.budgetbuddy.data

import android.util.Log
import com.budgetbuddy.data.dao.ChallengeDao
import com.budgetbuddy.data.dao.ExpenseDao
import com.budgetbuddy.data.entity.Challenge
import com.budgetbuddy.gamification.GamificationManager
import com.budgetbuddy.util.DateUtils

class ChallengeRepository(
    private val challengeDao: ChallengeDao,
    private val expenseDao: ExpenseDao,
    private val gamificationManager: GamificationManager
) {
    companion object {
        private const val TAG = "ChallengeRepository"

        const val TYPE_SAVE_R500 = "save_r500"
        const val TYPE_SPEND_LESS_1000 = "spend_less_1000"
        const val TYPE_WEEKEND_BUDGET = "weekend_budget"
        const val TYPE_NO_FAST_FOOD = "no_fast_food"

        val AVAILABLE_CHALLENGES = listOf(
            ChallengeTemplate(
                TYPE_SAVE_R500,
                "Save R500 Challenge",
                "Keep your monthly spending low enough to save at least R500.",
                500.0,
                75
            ),
            ChallengeTemplate(
                TYPE_SPEND_LESS_1000,
                "Spend Less Than R1000",
                "Limit your total spending to under R1000 this month.",
                1000.0,
                100
            ),
            ChallengeTemplate(
                TYPE_WEEKEND_BUDGET,
                "Weekend Budget Challenge",
                "Spend less than R500 over the weekend.",
                500.0,
                50
            ),
            ChallengeTemplate(
                TYPE_NO_FAST_FOOD,
                "No Fast-Food Challenge",
                "Avoid Food category expenses for 7 days.",
                0.0,
                60
            )
        )
    }

    data class ChallengeTemplate(
        val type: String,
        val title: String,
        val description: String,
        val targetAmount: Double,
        val rewardPoints: Int
    )

    suspend fun getActiveChallenges(userId: Int): List<Challenge> =
        challengeDao.getActiveChallenges(userId)

    suspend fun getCompletedChallenges(userId: Int): List<Challenge> =
        challengeDao.getCompletedChallenges(userId)

    suspend fun startChallenge(userId: Int, template: ChallengeTemplate): Challenge? {
        if (challengeDao.countActiveByType(userId, template.type) > 0) {
            Log.d(TAG, "Challenge ${template.type} already active for user $userId")
            return null
        }

        val endDate = when (template.type) {
            TYPE_WEEKEND_BUDGET -> DateUtils.toDisplay(DateUtils.endOfWeek())
            TYPE_NO_FAST_FOOD -> {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_MONTH, 7)
                DateUtils.formatDisplay(cal)
            }
            else -> DateUtils.toDisplay(DateUtils.endOfMonth())
        }

        val challenge = Challenge(
            userId = userId,
            challengeType = template.type,
            title = template.title,
            description = template.description,
            targetAmount = template.targetAmount,
            startDate = DateUtils.today(),
            endDate = endDate,
            rewardPoints = template.rewardPoints
        )
        val id = challengeDao.insert(challenge)
        Log.d(TAG, "Challenge started: ${template.title} (id=$id)")
        return challenge.copy(id = id.toInt())
    }

    suspend fun updateChallengeProgress(userId: Int) {
        val active = challengeDao.getActiveChallenges(userId)
        for (challenge in active) {
            val progress = calculateProgress(userId, challenge)
            val completed = isChallengeComplete(challenge, progress)
            val updated = challenge.copy(
                currentProgress = progress,
                isCompleted = completed
            )
            challengeDao.update(updated)
            if (completed) {
                Log.d(TAG, "Challenge completed: ${challenge.title}")
                gamificationManager.onExpenseLogged(userId, DateUtils.today())
            }
        }
    }

    private suspend fun calculateProgress(userId: Int, challenge: Challenge): Double {
        return when (challenge.challengeType) {
            TYPE_SAVE_R500 -> {
                val monthlySpent = expenseDao.getAllWithCategory(userId)
                    .filter { it.date.contains(DateUtils.currentMonthPattern()) }
                    .sumOf { it.amount }
                (5000.0 - monthlySpent).coerceAtLeast(0.0)
            }
            TYPE_SPEND_LESS_1000 -> {
                expenseDao.getAllWithCategory(userId)
                    .filter { it.date.contains(DateUtils.currentMonthPattern()) }
                    .sumOf { it.amount }
            }
            TYPE_WEEKEND_BUDGET -> {
                expenseDao.getSpentInRange(
                    userId,
                    DateUtils.startOfWeek(),
                    DateUtils.endOfWeek()
                )
            }
            TYPE_NO_FAST_FOOD -> {
                expenseDao.getCategorySpendingInRange(
                    userId,
                    "Food",
                    DateUtils.toSortable(challenge.startDate),
                    DateUtils.toSortable(challenge.endDate)
                )
            }
            else -> 0.0
        }
    }

    private fun isChallengeComplete(challenge: Challenge, progress: Double): Boolean {
        return when (challenge.challengeType) {
            TYPE_SAVE_R500 -> progress >= challenge.targetAmount
            TYPE_SPEND_LESS_1000 -> progress < challenge.targetAmount
            TYPE_WEEKEND_BUDGET -> progress <= challenge.targetAmount
            TYPE_NO_FAST_FOOD -> progress == 0.0 &&
                DateUtils.daysBetween(challenge.startDate, DateUtils.today()) >= 7
            else -> false
        }
    }
}
