package com.budgetbuddy.insights

import android.util.Log
import com.budgetbuddy.data.dao.CategorySpending
import com.budgetbuddy.data.dao.ExpenseDao
import com.budgetbuddy.util.CurrencyUtils
import com.budgetbuddy.util.DateUtils

/**
 * Financial insights engine — analyses spending patterns and generates actionable tips.
 */
class InsightsManager(private val expenseDao: ExpenseDao) {

    companion object {
        private const val TAG = "InsightsManager"
    }

    data class FinancialInsight(
        val title: String,
        val description: String,
        val type: InsightType
    )

    enum class InsightType {
        INFO, WARNING, SUGGESTION, POSITIVE
    }

    private suspend fun getMonthlySpentForPattern(userId: Int, monthPattern: String): Double {
        return expenseDao.getAllWithCategory(userId)
            .filter { it.date.contains(monthPattern) }
            .sumOf { it.amount }
    }

    suspend fun generateInsights(userId: Int, monthlyBudget: Double): List<FinancialInsight> {
        Log.d(TAG, "Generating insights for user $userId")
        val insights = mutableListOf<FinancialInsight>()
        val monthPattern = DateUtils.currentMonthPattern()
        val monthlySpent = getMonthlySpentForPattern(userId, monthPattern)
        val categorySpending = expenseDao.getSpendingByCategoryAll(userId)

        if (categorySpending.isEmpty()) {
            insights.add(
                FinancialInsight(
                    "Get Started",
                    "Add your first expense to unlock personalised financial insights.",
                    InsightType.INFO
                )
            )
            return insights
        }

        val highest = categorySpending.maxByOrNull { it.total }
        val lowest = categorySpending.filter { it.total > 0 }.minByOrNull { it.total }

        highest?.let {
            insights.add(
                FinancialInsight(
                    "Highest Spending Category",
                    "You spend the most on ${it.categoryName}: ${CurrencyUtils.toRand(it.total)} this month.",
                    InsightType.INFO
                )
            )
        }

        lowest?.let {
            insights.add(
                FinancialInsight(
                    "Lowest Spending Category",
                    "Your lowest spending category is ${it.categoryName}: ${CurrencyUtils.toRand(it.total)}.",
                    InsightType.INFO
                )
            )
        }

        val daysElapsed = DateUtils.dayOfMonth().coerceAtLeast(1)
        val avgDaily = monthlySpent / daysElapsed
        insights.add(
            FinancialInsight(
                "Average Daily Spend",
                "You average ${CurrencyUtils.toRand(avgDaily)} per day this month.",
                InsightType.INFO
            )
        )

        val projectedMonthly = avgDaily * DateUtils.daysInCurrentMonth()
        if (projectedMonthly > monthlyBudget) {
            insights.add(
                FinancialInsight(
                    "Spending Warning",
                    "At this rate you'll spend ${CurrencyUtils.toRand(projectedMonthly)} — over your ${CurrencyUtils.toRand(monthlyBudget)} budget.",
                    InsightType.WARNING
                )
            )
        }

        val remaining = monthlyBudget - monthlySpent
        if (remaining > 0) {
            insights.add(
                FinancialInsight(
                    "Savings Suggestion",
                    "You have ${CurrencyUtils.toRand(remaining)} left. Consider saving 10% (${CurrencyUtils.toRand(remaining * 0.1)}) this month.",
                    InsightType.SUGGESTION
                )
            )
        }

        if (monthlySpent <= monthlyBudget * 0.5 && daysElapsed > 15) {
            insights.add(
                FinancialInsight(
                    "Great Progress!",
                    "You're under 50% of your budget halfway through the month. Keep it up!",
                    InsightType.POSITIVE
                )
            )
        }

        val trend = calculateMonthlyTrend(userId, monthlySpent)
        insights.add(
            FinancialInsight(
                "Monthly Trend",
                trend,
                if (monthlySpent > monthlyBudget) InsightType.WARNING else InsightType.INFO
            )
        )

        return insights
    }

    suspend fun getHighestCategory(userId: Int): CategorySpending? {
        return expenseDao.getSpendingByCategoryAll(userId).maxByOrNull { it.total }
    }

    suspend fun getLowestCategory(userId: Int): CategorySpending? {
        return expenseDao.getSpendingByCategoryAll(userId).filter { it.total > 0 }.minByOrNull { it.total }
    }

    suspend fun getAverageDailySpend(userId: Int): Double {
        val monthPattern = DateUtils.currentMonthPattern()
        val monthlySpent = getMonthlySpentForPattern(userId, monthPattern)
        return monthlySpent / DateUtils.dayOfMonth().coerceAtLeast(1)
    }

    private suspend fun calculateMonthlyTrend(userId: Int, currentSpent: Double): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -1)
        val lastMonthPattern = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
            .format(cal.time)
        val lastMonthSpent = getMonthlySpentForPattern(userId, lastMonthPattern)

        return when {
            lastMonthSpent == 0.0 -> "No previous month data to compare."
            currentSpent > lastMonthSpent -> {
                val increase = ((currentSpent - lastMonthSpent) / lastMonthSpent * 100).toInt()
                "Spending is up $increase% compared to last month."
            }
            currentSpent < lastMonthSpent -> {
                val decrease = ((lastMonthSpent - currentSpent) / lastMonthSpent * 100).toInt()
                "Spending is down $decrease% compared to last month. Well done!"
            }
            else -> "Spending is the same as last month."
        }
    }
}
