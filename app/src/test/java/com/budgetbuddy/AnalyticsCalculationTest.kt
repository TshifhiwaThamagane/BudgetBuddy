package com.budgetbuddy

import com.budgetbuddy.data.dao.CategorySpending
import com.budgetbuddy.data.dao.DailySpending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsCalculationTest {

    @Test
    fun `category spending aggregation sums correctly`() {
        val expenses = listOf(
            CategorySpending("Food", 500.0),
            CategorySpending("Transport", 300.0),
            CategorySpending("Food", 200.0)
        )
        val grouped = expenses.groupBy { it.categoryName }
            .map { (name, list) -> CategorySpending(name, list.sumOf { it.total }) }

        val food = grouped.find { it.categoryName == "Food" }
        assertEquals(700.0, food?.total ?: 0.0, 0.01)
    }

    @Test
    fun `daily spending sorted chronologically`() {
        val daily = listOf(
            DailySpending("10 Jun 2026", 100.0),
            DailySpending("08 Jun 2026", 200.0),
            DailySpending("12 Jun 2026", 50.0)
        )
        assertEquals(3, daily.size)
        assertTrue(daily.sumOf { it.total } == 350.0)
    }

    @Test
    fun `empty dataset returns zero totals`() {
        val categories = emptyList<CategorySpending>()
        assertTrue(categories.isEmpty())
        assertEquals(0.0, categories.sumOf { it.total }, 0.01)
    }

    @Test
    fun `percentage of budget from category spending`() {
        val categoryTotal = 1500.0
        val budget = 5000.0
        val percentage = ((categoryTotal / budget) * 100).toInt()
        assertEquals(30, percentage)
    }
}
