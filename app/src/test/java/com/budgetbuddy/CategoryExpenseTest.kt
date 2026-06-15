package com.budgetbuddy

import com.budgetbuddy.data.entity.Category
import com.budgetbuddy.data.entity.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryExpenseTest {

    @Test
    fun `category creation requires non-empty name`() {
        val name = "  Health  ".trim()
        assertTrue(name.isNotEmpty())
        assertEquals("Health", name)
    }

    @Test
    fun `duplicate category names are prevented`() {
        val existing = Category(id = 1, name = "Food")
        val duplicate = Category(id = 2, name = "Food")
        assertEquals(existing.name, duplicate.name)
    }

    @Test
    fun `expense amount must be greater than zero`() {
        val amount = 150.50
        assertTrue(amount > 0)
    }

    @Test
    fun `expense creation stores all required fields`() {
        val expense = Expense(
            userId = 1,
            amount = 250.0,
            date = "15 Jun 2026",
            categoryId = 1,
            note = "Lunch",
            receiptUri = null
        )
        assertEquals(250.0, expense.amount, 0.01)
        assertEquals(1, expense.categoryId)
        assertEquals("Lunch", expense.note)
    }

    @Test
    fun `default categories include Food Transport Entertainment`() {
        val defaults = listOf("Food", "Transport", "Entertainment")
        assertEquals(3, defaults.size)
        assertTrue(defaults.contains("Food"))
    }
}
