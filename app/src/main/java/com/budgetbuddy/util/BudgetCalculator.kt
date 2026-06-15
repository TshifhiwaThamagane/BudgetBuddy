package com.budgetbuddy.util

enum class BudgetStatus {
    SAFE,
    WARNING,
    DANGER
}

object BudgetCalculator {

    fun calculatePercentage(spent: Double, budget: Double): Int {
        if (budget <= 0) return 0
        return ((spent / budget) * 100).toInt().coerceIn(0, 100)
    }

    fun calculateRemaining(spent: Double, budget: Double): Double = budget - spent

    fun determineStatus(spent: Double, maxGoal: Double, monthlyBudget: Double): BudgetStatus {
        return when {
            spent > monthlyBudget -> BudgetStatus.DANGER
            spent >= maxGoal -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
    }

    fun xpForLevel(level: Int): Int = level * 100

    fun levelFromXp(xp: Int): Int {
        var level = 1
        var threshold = 100
        var remaining = xp
        while (remaining >= threshold) {
            remaining -= threshold
            level++
            threshold = level * 100
        }
        return level
    }

    fun xpProgressInLevel(xp: Int, level: Int): Int {
        var consumed = 0
        for (l in 1 until level) {
            consumed += l * 100
        }
        return xp - consumed
    }

    fun xpNeededForNextLevel(level: Int): Int = level * 100
}
