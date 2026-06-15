package com.budgetbuddy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.budgetbuddy.data.dao.AchievementDao
import com.budgetbuddy.data.dao.BadgeDao
import com.budgetbuddy.data.dao.BudgetGoalDao
import com.budgetbuddy.data.dao.CategoryDao
import com.budgetbuddy.data.dao.ChallengeDao
import com.budgetbuddy.data.dao.ExpenseDao
import com.budgetbuddy.data.dao.GamificationDao
import com.budgetbuddy.data.dao.StreakDao
import com.budgetbuddy.data.dao.UserDao
import com.budgetbuddy.data.entity.Achievement
import com.budgetbuddy.data.entity.BudgetGoal
import com.budgetbuddy.data.entity.Category
import com.budgetbuddy.data.entity.Challenge
import com.budgetbuddy.data.entity.Expense
import com.budgetbuddy.data.entity.User
import com.budgetbuddy.data.entity.UserBadge
import com.budgetbuddy.data.entity.UserGamification
import com.budgetbuddy.data.entity.UserStreak

@Database(
    entities = [
        User::class,
        Category::class,
        Expense::class,
        BudgetGoal::class,
        UserGamification::class,
        UserBadge::class,
        Achievement::class,
        UserStreak::class,
        Challenge::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun badgeDao(): BadgeDao
    abstract fun achievementDao(): AchievementDao
    abstract fun streakDao(): StreakDao
    abstract fun challengeDao(): ChallengeDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_buddy.db"
                )
                    .addMigrations(DatabaseMigrations.MIGRATION_2_3)
                    .build()
                    .also { db -> instance = db }
            }
        }
    }
}
