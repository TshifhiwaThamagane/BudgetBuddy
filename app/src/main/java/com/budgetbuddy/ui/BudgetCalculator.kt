package com.budgetbuddy.util

import android.util.Log

/**
 * BudgetStatus — represents the three possible spending health states.
 *
 *  SAFE    → spending is below the user's max goal
 *  WARNING → spending has reached or exceeded the max goal but is still under the monthly budget
 *  DANGER  → spending has exceeded the monthly budget
 */
enum class BudgetStatus {
    SAFE,
    WARNING,
    DANGER
}

/**
 * BudgetCalculator — pure utility object for budget maths and XP/level calculations.
 *
 * Kept as a Kotlin object (singleton) because none of these functions require
 * state or Android context — they work purely on the values passed in.
 *
 * References:
 *  - Kotlin object declarations: https://kotlinlang.org/docs/object-declarations.html
 */
object BudgetCalculator {

    private const val TAG = "BudgetCalculator"

    /**
     * Calculates what percentage of the monthly budget has been spent.
     * Clamps the result to [0, 100] to keep the progress bar safe from overflow.
     *
     * @param spent  Total amount spent this month.
     * @param budget Total monthly budget.
     * @return Integer percentage in the range 0–100.
     */
    fun calculatePercentage(spent: Double, budget: Double): Int {
        if (budget <= 0) {
            Log.d(TAG, "calculatePercentage: budget is 0 or negative, returning 0")
            return 0
        }
        val pct = ((spent / budget) * 100).toInt().coerceIn(0, 100)
        Log.d(TAG, "calculatePercentage: spent=$spent, budget=$budget -> $pct%")
        return pct
    }

    /**
     * Returns how much of the budget is still available (can be negative if over budget).
     *
     * @param spent  Total amount spent this month.
     * @param budget Total monthly budget.
     * @return Remaining amount (budget - spent).
     */
    fun calculateRemaining(spent: Double, budget: Double): Double {
        val remaining = budget - spent
        Log.d(TAG, "calculateRemaining: $budget - $spent = $remaining")
        return remaining
    }

    /**
     * Determines the [BudgetStatus] based on spending relative to goals.
     *
     * Logic:
     *  - DANGER  if spent exceeds the monthly budget cap.
     *  - WARNING if spent has reached or passed the user's max spending goal.
     *  - SAFE    otherwise.
     *
     * @param spent         Total spent this month.
     * @param maxGoal       Upper spending threshold (triggers WARNING).
     * @param monthlyBudget Hard budget cap (triggers DANGER).
     */
    fun determineStatus(spent: Double, maxGoal: Double, monthlyBudget: Double): BudgetStatus {
        val status = when {
            spent > monthlyBudget -> BudgetStatus.DANGER
            spent >= maxGoal -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
        Log.d(TAG, "determineStatus: spent=$spent, maxGoal=$maxGoal, budget=$monthlyBudget -> $status")
        return status
    }

    /**
     * Returns the total XP needed to complete a given level.
     * Each level requires (level × 100) XP — so level 1 = 100 XP, level 2 = 200 XP, etc.
     */
    fun xpForLevel(level: Int): Int = level * 100

    /**
     * Derives the user's current level from their cumulative XP total.
     * Iterates through level thresholds, consuming XP until the remainder
     * is less than the next threshold.
     *
     * @param xp Cumulative XP earned by the user.
     * @return Current level (minimum 1).
     */
    fun levelFromXp(xp: Int): Int {
        var level = 1
        var threshold = 100
        var remaining = xp
        // Keep levelling up while the user has enough XP for the next threshold
        while (remaining >= threshold) {
            remaining -= threshold
            level++
            threshold = level * 100
        }
        Log.d(TAG, "levelFromXp: xp=$xp -> level=$level")
        return level
    }

    /**
     * Returns how much XP the user has accumulated within their current level
     * (i.e., progress toward the next level).
     *
     * @param xp    Total cumulative XP.
     * @param level The user's current level (from [levelFromXp]).
     * @return XP progress within the current level.
     */
    fun xpProgressInLevel(xp: Int, level: Int): Int {
        var consumed = 0
        // Sum up all XP consumed by previous levels
        for (l in 1 until level) {
            consumed += l * 100
        }
        return xp - consumed
    }

    /**
     * Returns the total XP required to advance from the current level to the next.
     */
    fun xpNeededForNextLevel(level: Int): Int = level * 100
}
