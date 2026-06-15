package com.budgetbuddy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetbuddy.data.entity.Expense

data class ExpenseWithCategory(
    val id: Int,
    val amount: Double,
    val date: String,
    val note: String,
    val receiptUri: String?,
    val categoryName: String,
    val categoryId: Int = 0
)

data class CategorySpending(
    val categoryName: String,
    val total: Double
)

data class DailySpending(
    val date: String,
    val total: Double
)

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense): Long

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Int): Expense?

    @Query("UPDATE expenses SET receiptUri = :receiptUri WHERE id = :id")
    suspend fun updateReceipt(id: Int, receiptUri: String?)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query(
        """
        SELECT expenses.id, expenses.amount, expenses.date, expenses.note, expenses.receiptUri,
               categories.name AS categoryName, expenses.categoryId
        FROM expenses
        INNER JOIN categories ON categories.id = expenses.categoryId
        WHERE expenses.userId = :userId OR expenses.userId = 0
        ORDER BY expenses.id DESC
        """
    )
    suspend fun getAllWithCategory(userId: Int): List<ExpenseWithCategory>

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0) FROM expenses
        WHERE (userId = :userId OR userId = 0)
        """
    )
    suspend fun getTotalSpent(userId: Int): Double

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0) FROM expenses
        WHERE (userId = :userId OR userId = 0) AND date LIKE :monthPattern || '%'
        """
    )
    suspend fun getMonthlySpent(userId: Int, monthPattern: String): Double

    @Query("SELECT COUNT(*) FROM expenses WHERE userId = :userId OR userId = 0")
    suspend fun getExpenseCount(userId: Int): Int

    @Query(
        """
        SELECT categories.name AS categoryName, IFNULL(SUM(expenses.amount), 0) AS total
        FROM expenses
        INNER JOIN categories ON categories.id = expenses.categoryId
        WHERE (expenses.userId = :userId OR expenses.userId = 0)
        GROUP BY categories.name
        ORDER BY total DESC
        """
    )
    suspend fun getSpendingByCategoryAll(userId: Int): List<CategorySpending>

    @Query(
        """
        SELECT categories.name AS categoryName, IFNULL(SUM(expenses.amount), 0) AS total
        FROM expenses
        INNER JOIN categories ON categories.id = expenses.categoryId
        WHERE (expenses.userId = :userId OR expenses.userId = 0)
          AND expenses.date >= :startDate AND expenses.date <= :endDate
        GROUP BY categories.name
        ORDER BY total DESC
        """
    )
    suspend fun getSpendingByCategoryInRange(
        userId: Int,
        startDate: String,
        endDate: String
    ): List<CategorySpending>

    @Query(
        """
        SELECT expenses.date AS date, IFNULL(SUM(expenses.amount), 0) AS total
        FROM expenses
        WHERE (expenses.userId = :userId OR expenses.userId = 0)
          AND expenses.date >= :startDate AND expenses.date <= :endDate
        GROUP BY expenses.date
        ORDER BY expenses.date ASC
        """
    )
    suspend fun getDailySpendingInRange(
        userId: Int,
        startDate: String,
        endDate: String
    ): List<DailySpending>

    @Query(
        """
        SELECT DISTINCT date FROM expenses
        WHERE userId = :userId OR userId = 0
        ORDER BY date DESC
        """
    )
    suspend fun getDistinctDates(userId: Int): List<String>

    @Query(
        """
        SELECT COUNT(DISTINCT categoryId) FROM expenses
        WHERE userId = :userId OR userId = 0
        """
    )
    suspend fun getDistinctCategoryCount(userId: Int): Int

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0) FROM expenses
        INNER JOIN categories ON categories.id = expenses.categoryId
        WHERE (expenses.userId = :userId OR expenses.userId = 0)
          AND categories.name = :categoryName
          AND expenses.date >= :startDate AND expenses.date <= :endDate
        """
    )
    suspend fun getCategorySpendingInRange(
        userId: Int,
        categoryName: String,
        startDate: String,
        endDate: String
    ): Double

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0) FROM expenses
        WHERE (userId = :userId OR userId = 0)
          AND date >= :startDate AND date <= :endDate
        """
    )
    suspend fun getSpentInRange(userId: Int, startDate: String, endDate: String): Double
}
