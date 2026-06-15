package com.budgetbuddy.gamification

/**
 * Badge definitions for the gamification system.
 * Each badge has a unique ID, display name, description, and XP reward.
 */
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val xpReward: Int,
    val iconEmoji: String
) {
    companion object {
        const val FIRST_EXPENSE = "first_expense"
        const val EXPENSE_EXPLORER = "expense_explorer"
        const val SEVEN_DAY_STREAK = "seven_day_streak"
        const val THIRTY_DAY_STREAK = "thirty_day_streak"
        const val BUDGET_MASTER = "budget_master"
        const val SAVINGS_HERO = "savings_hero"
        const val CATEGORY_CHAMPION = "category_champion"

        val ALL_BADGES = listOf(
            Badge(FIRST_EXPENSE, "First Expense", "Log your first expense", 10, "🎯"),
            Badge(EXPENSE_EXPLORER, "Expense Explorer", "Log 10 expenses", 25, "🧭"),
            Badge(SEVEN_DAY_STREAK, "7 Day Streak", "Log expenses 7 days in a row", 50, "🔥"),
            Badge(THIRTY_DAY_STREAK, "30 Day Streak", "Log expenses 30 days in a row", 150, "⭐"),
            Badge(BUDGET_MASTER, "Budget Master", "Stay within your monthly budget", 75, "👑"),
            Badge(SAVINGS_HERO, "Savings Hero", "Save at least 20% of your budget", 100, "💰"),
            Badge(CATEGORY_CHAMPION, "Category Champion", "Use all default categories", 40, "🏆")
        )

        fun findById(badgeId: String): Badge? = ALL_BADGES.find { it.id == badgeId }
    }
}
