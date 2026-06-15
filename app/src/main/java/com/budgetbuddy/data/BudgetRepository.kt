package com.budgetbuddy.data

import android.content.Context
import com.budgetbuddy.data.dao.ExpenseWithCategory
import com.budgetbuddy.data.entity.BudgetGoal
import com.budgetbuddy.data.entity.Category
import com.budgetbuddy.data.entity.Challenge
import com.budgetbuddy.data.entity.Expense
import com.budgetbuddy.data.entity.User
import com.budgetbuddy.gamification.GamificationManager
import com.budgetbuddy.insights.InsightsManager
import com.budgetbuddy.util.DateUtils

class BudgetRepository private constructor(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val categoryDao = db.categoryDao()
    private val expenseDao = db.expenseDao()
    private val budgetGoalDao = db.budgetGoalDao()

    val gamificationManager = GamificationManager(
        db.gamificationDao(),
        db.badgeDao(),
        db.achievementDao(),
        db.streakDao(),
        expenseDao
    )
    val insightsManager = InsightsManager(expenseDao)
    val challengeRepository = ChallengeRepository(db.challengeDao(), expenseDao, gamificationManager)

    // --- Auth ---

    suspend fun registerUser(fullName: String, email: String, password: String): Result<User> {
        val existing = userDao.getByEmail(email)
        if (existing != null) return Result.failure(IllegalArgumentException("Email already exists"))
        val id = userDao.insert(User(fullName = fullName, email = email, password = password)).toInt()
        val user = User(id = id, fullName = fullName, email = email, password = password)
        ensureUserDefaults(id)
        return Result.success(user)
    }

    suspend fun login(email: String, password: String): User? = userDao.login(email, password)

    suspend fun resetPassword(email: String, newPassword: String): Boolean {
        val user = userDao.getByEmail(email) ?: return false
        userDao.update(user.copy(password = newPassword))
        return true
    }

    // --- User defaults ---

    suspend fun ensureUserDefaults(userId: Int) {
        if (budgetGoalDao.getByUserId(userId) == null) {
            budgetGoalDao.upsert(BudgetGoal(userId = userId))
        }
        gamificationManager.ensureUserProfile(userId)
    }

    // --- Budget Goals ---

    suspend fun getBudgetGoal(userId: Int): BudgetGoal {
        ensureUserDefaults(userId)
        return budgetGoalDao.getByUserId(userId)!!
    }

    suspend fun saveBudgetGoal(userId: Int, monthlyBudget: Double, minGoal: Double, maxGoal: Double) {
        budgetGoalDao.upsert(
            BudgetGoal(
                userId = userId,
                monthlyBudget = monthlyBudget,
                minGoal = minGoal,
                maxGoal = maxGoal
            )
        )
    }

    // --- Categories ---

    suspend fun ensureDefaultCategories() {
        if (categoryDao.count() == 0) {
            listOf("Food", "Transport", "Entertainment").forEach {
                categoryDao.insert(Category(name = it))
            }
        }
    }

    suspend fun getCategories(): List<Category> = categoryDao.getAll()

    suspend fun addCategory(name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "Category name is required" }
        if (categoryDao.findByName(normalized) != null) {
            throw IllegalArgumentException("Category already exists")
        }
        val id = categoryDao.insert(Category(name = normalized))
        if (id == -1L) throw IllegalArgumentException("Category already exists")
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }

    // --- Expenses ---

    suspend fun addExpense(
        userId: Int,
        amount: Double,
        date: String,
        categoryId: Int,
        note: String,
        receiptUri: String?
    ): GamificationManager.UnlockResult {
        expenseDao.insert(
            Expense(
                userId = userId,
                amount = amount,
                date = date,
                categoryId = categoryId,
                note = note,
                receiptUri = receiptUri
            )
        )
        val unlockResult = gamificationManager.onExpenseLogged(userId, date)
        challengeRepository.updateChallengeProgress(userId)
        val goal = getBudgetGoal(userId)
        val monthlySpent = getMonthlySpent(userId)
        gamificationManager.checkBudgetBadges(userId, monthlySpent, goal.monthlyBudget)
        return unlockResult
    }

    suspend fun getAllExpenses(userId: Int): List<ExpenseWithCategory> =
        expenseDao.getAllWithCategory(userId)

    suspend fun getTotalSpent(userId: Int): Double = expenseDao.getTotalSpent(userId)

    suspend fun getMonthlySpent(userId: Int): Double {
        val monthPattern = DateUtils.currentMonthPattern()
        return expenseDao.getAllWithCategory(userId)
            .filter { it.date.contains(monthPattern) }
            .sumOf { it.amount }
    }

    suspend fun getExpenseById(id: Int): Expense? = expenseDao.getById(id)

    suspend fun updateReceipt(expenseId: Int, receiptUri: String?) {
        expenseDao.updateReceipt(expenseId, receiptUri)
    }

    suspend fun deleteExpense(id: Int) {
        expenseDao.deleteById(id)
    }

    // --- Analytics ---

    suspend fun getSpendingByCategory(userId: Int, startDate: String, endDate: String) =
        filterAndAggregateByCategory(userId, startDate, endDate)

    suspend fun getDailySpending(userId: Int, startDate: String, endDate: String) =
        filterAndAggregateByDay(userId, startDate, endDate)

    private suspend fun filterAndAggregateByCategory(
        userId: Int,
        startDisplay: String,
        endDisplay: String
    ): List<com.budgetbuddy.data.dao.CategorySpending> {
        val start = DateUtils.toSortable(startDisplay)
        val end = DateUtils.toSortable(endDisplay)
        return expenseDao.getAllWithCategory(userId)
            .filter { DateUtils.isInRange(it.date, start, end) }
            .groupBy { it.categoryName }
            .map { (name, list) ->
                com.budgetbuddy.data.dao.CategorySpending(name, list.sumOf { e -> e.amount })
            }
            .sortedByDescending { it.total }
    }

    private suspend fun filterAndAggregateByDay(
        userId: Int,
        startDisplay: String,
        endDisplay: String
    ): List<com.budgetbuddy.data.dao.DailySpending> {
        val start = DateUtils.toSortable(startDisplay)
        val end = DateUtils.toSortable(endDisplay)
        return expenseDao.getAllWithCategory(userId)
            .filter { DateUtils.isInRange(it.date, start, end) }
            .groupBy { it.date }
            .map { (date, list) ->
                com.budgetbuddy.data.dao.DailySpending(date, list.sumOf { e -> e.amount })
            }
            .sortedBy { DateUtils.toSortable(it.date) }
    }

    companion object {
        @Volatile
        private var instance: BudgetRepository? = null

        fun getInstance(context: Context): BudgetRepository {
            return instance ?: synchronized(this) {
                instance ?: BudgetRepository(context.applicationContext).also { repo ->
                    instance = repo
                }
            }
        }
    }
}
