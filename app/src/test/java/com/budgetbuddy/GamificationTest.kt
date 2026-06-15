package com.budgetbuddy

import com.budgetbuddy.gamification.Badge
import com.budgetbuddy.util.BudgetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationTest {

    @Test
    fun `all seven badges are defined`() {
        assertEquals(7, Badge.ALL_BADGES.size)
    }

    @Test
    fun `first expense badge has correct properties`() {
        val badge = Badge.findById(Badge.FIRST_EXPENSE)
        assertNotNull(badge)
        assertEquals("First Expense", badge?.name)
        assertEquals(10, badge?.xpReward)
    }

    @Test
    fun `seven day streak badge exists`() {
        val badge = Badge.findById(Badge.SEVEN_DAY_STREAK)
        assertNotNull(badge)
        assertEquals(50, badge?.xpReward)
    }

    @Test
    fun `budget master badge exists`() {
        val badge = Badge.findById(Badge.BUDGET_MASTER)
        assertNotNull(badge)
        assertEquals("Budget Master", badge?.name)
    }

    @Test
    fun `XP level progression works`() {
        val level1 = BudgetCalculator.levelFromXp(50)
        val level2 = BudgetCalculator.levelFromXp(150)
        assertEquals(1, level1)
        assertTrue(level2 >= 2)
    }

    @Test
    fun `xp needed for next level increases`() {
        assertEquals(100, BudgetCalculator.xpNeededForNextLevel(1))
        assertEquals(200, BudgetCalculator.xpNeededForNextLevel(2))
    }
}
