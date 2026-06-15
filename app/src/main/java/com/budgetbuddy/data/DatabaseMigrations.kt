package com.budgetbuddy.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add userId column to existing expenses table (preserves data)
            db.execSQL("ALTER TABLE expenses ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS budget_goals (
                    userId INTEGER NOT NULL PRIMARY KEY,
                    monthlyBudget REAL NOT NULL,
                    minGoal REAL NOT NULL,
                    maxGoal REAL NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_gamification (
                    userId INTEGER NOT NULL PRIMARY KEY,
                    xp INTEGER NOT NULL,
                    level INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_badges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    badgeId TEXT NOT NULL,
                    unlockedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_user_badges_userId_badgeId ON user_badges (userId, badgeId)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS achievements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    xpEarned INTEGER NOT NULL,
                    achievedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_streaks (
                    userId INTEGER NOT NULL PRIMARY KEY,
                    dailyStreak INTEGER NOT NULL,
                    weeklyStreak INTEGER NOT NULL,
                    bestStreak INTEGER NOT NULL,
                    lastLogDate TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS challenges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    challengeType TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    targetAmount REAL NOT NULL,
                    currentProgress REAL NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT NOT NULL,
                    isCompleted INTEGER NOT NULL,
                    rewardPoints INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}
