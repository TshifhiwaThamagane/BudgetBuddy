package com.budgetbuddy

import com.budgetbuddy.util.BudgetCalculator
import com.budgetbuddy.util.BudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalCalculationTest {

    @Test
    fun `calculate percentage used correctly`() {
        assertEquals(50, BudgetCalculator.calculatePercentage(2500.0, 5000.0))
        assertEquals(100, BudgetCalculator.calculatePercentage(5000.0, 5000.0))
        assertEquals(0, BudgetCalculator.calculatePercentage(0.0, 5000.0))
    }

    @Test
    fun `calculate remaining budget correctly`() {
        assertEquals(3000.0, BudgetCalculator.calculateRemaining(2000.0, 5000.0), 0.01)
        assertEquals(-500.0, BudgetCalculator.calculateRemaining(5500.0, 5000.0), 0.01)
    }

    @Test
    fun `status is SAFE when under max goal`() {
        val status = BudgetCalculator.determineStatus(3000.0, 4500.0, 5000.0)
        assertEquals(BudgetStatus.SAFE, status)
    }

    @Test
    fun `status is WARNING when between max goal and budget`() {
        val status = BudgetCalculator.determineStatus(4600.0, 4500.0, 5000.0)
        assertEquals(BudgetStatus.WARNING, status)
    }

    @Test
    fun `status is DANGER when over monthly budget`() {
        val status = BudgetCalculator.determineStatus(5500.0, 4500.0, 5000.0)
        assertEquals(BudgetStatus.DANGER, status)
    }

    @Test
    fun `level calculation from XP`() {
        assertEquals(1, BudgetCalculator.levelFromXp(0))
        assertEquals(1, BudgetCalculator.levelFromXp(99))
        assertEquals(2, BudgetCalculator.levelFromXp(100))
        assertEquals(3, BudgetCalculator.levelFromXp(500))
    }
}
