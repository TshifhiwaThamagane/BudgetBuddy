package com.budgetbuddy.data

import android.content.Context
import android.util.Log
import com.budgetbuddy.data.dao.ExpenseWithCategory
import com.budgetbuddy.data.entity.BudgetGoal
import com.budgetbuddy.data.entity.Category
import com.budgetbuddy.data.entity.Challenge
import com.budgetbuddy.data.entity.Expense
import com.budgetbuddy.data.entity.User
import com.budgetbuddy.gamification.GamificationManager
import com.budgetbuddy.insights.InsightsManager
import com.budgetbuddy.util.DateUtils

/**
 * BudgetRepository — the single source of truth for all data operations.
 *
 * Acts as the mediator between the DAO layer (Room database) and the ViewModels.
 * All suspend functions must be called from a coroutine (e.g. viewModelScope or lifecycleScope).
 *
 * Uses the Singleton pattern (via the companion object) to ensure only one
 * repository instance exists across the entire app lifecycle.
 *
 * References:
 *  - Repository pattern: https://developer.android.com/topic/architecture/data-layer
 *  - Room DAO: https://developer.android.com/training/data-storage/room/accessing-data
 *  - Kotlin Singleton: https://kotlinlang.org/docs/object-declarations.html
 */
class BudgetRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "BudgetRepository"

        // @Volatile ensures the instance field is always read from main memory,
        // preventing stale cached values in a multi-threaded environment.
        @Volatile
        private var instance: BudgetRepository? = null

        /**
         * Returns the existing singleton or creates a new one inside a synchronized block.
         * Double-checked locking avoids unnecessary synchronization after the first creation.
         */
        fun getInstance(context: Context): BudgetRepository {
            return instance ?: synchronized(this) {
                instance ?: BudgetRepository(context.applicationContext).also { repo ->
                    Log.d(TAG, "BudgetRepository singleton created")
                    instance = repo
                }
            }
        }
    }

    // Obtain the Room database singleton and extract all DAOs
    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val categoryDao = db.categoryDao()
    private val expenseDao = db.expenseDao()
    private val budgetGoalDao = db.budgetGoalDao()

    // Higher-level managers wired up with their required DAOs
    val gamificationManager = GamificationManager(
        db.gamificationDao(),
        db.badgeDao(),
        db.achievementDao(),
        db.streakDao(),
        expenseDao
    )
    val insightsManager = InsightsManager(expenseDao)
    val challengeRepository = ChallengeRepository(db.challengeDao(), expenseDao, gamificationManager)

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    /**
     * Registers a new user after checking the email is not already taken.
     * Returns a Result<User> so the caller can handle success and failure uniformly.
     */
    suspend fun registerUser(fullName: String, email: String, password: String): Result<User> {
        Log.d(TAG, "registerUser: attempting registration for $email")
        val existing = userDao.getByEmail(email)
        if (existing != null) {
            Log.e(TAG, "registerUser: email already exists -> $email")
            return Result.failure(IllegalArgumentException("Email already exists"))
        }
        val id = userDao.insert(User(fullName = fullName, email = email, password = password)).toInt()
        val user = User(id = id, fullName = fullName, email = email, password = password)
        ensureUserDefaults(id)
        Log.d(TAG, "registerUser: success, new userId=$id")
        return Result.success(user)
    }

    /**
     * Attempts to log in using the provided credentials.
     * Returns the User if found, or null if the email/password combination is incorrect.
     */
    suspend fun login(email: String, password: String): User? {
        Log.d(TAG, "login: looking up user for email=$email")
        val user = userDao.login(email, password)
        if (user == null) Log.e(TAG, "login: no match found for $email")
        else Log.d(TAG, "login: found userId=${user.id}")
        return user
    }

    /**
     * Resets the password for the account associated with the given email.
     * Returns false if no matching account is found.
     */
    suspend fun resetPassword(email: String, newPassword: String): Boolean {
        Log.d(TAG, "resetPassword: looking up account for $email")
        val user = userDao.getByEmail(email)
        if (user == null) {
            Log.e(TAG, "resetPassword: no account found for $email")
            return false
        }
        userDao.update(user.copy(password = newPassword))
        Log.d(TAG, "resetPassword: password updated for userId=${user.id}")
        return true
    }

    // -------------------------------------------------------------------------
    // User defaults
    // -------------------------------------------------------------------------

    /**
     * Seeds a new user with a default budget goal and a gamification profile
     * if they don't already have one. Called after registration and on dashboard load.
     */
    suspend fun ensureUserDefaults(userId: Int) {
        if (budgetGoalDao.getByUserId(userId) == null) {
            Log.d(TAG, "ensureUserDefaults: creating default BudgetGoal for userId=$userId")
            budgetGoalDao.upsert(BudgetGoal(userId = userId))
        }
        gamificationManager.ensureUserProfile(userId)
    }

    // -------------------------------------------------------------------------
    // Budget Goals
    // -------------------------------------------------------------------------

    /**
     * Retrieves the user's current budget goal, creating a default one if absent.
     */
    suspend fun getBudgetGoal(userId: Int): BudgetGoal {
        ensureUserDefaults(userId)
        val goal = budgetGoalDao.getByUserId(userId)!!
        Log.d(TAG, "getBudgetGoal: userId=$userId -> monthly=${goal.monthlyBudget}")
        return goal
    }

    /**
     * Persists updated budget goal values for the given user.
     * Uses upsert so it works whether or not a row already exists.
     */
    suspend fun saveBudgetGoal(userId: Int, monthlyBudget: Double, minGoal: Double, maxGoal: Double) {
        Log.d(TAG, "saveBudgetGoal: userId=$userId, monthly=$monthlyBudget, min=$minGoal, max=$maxGoal")
        budgetGoalDao.upsert(
            BudgetGoal(
                userId = userId,
                monthlyBudget = monthlyBudget,
                minGoal = minGoal,
                maxGoal = maxGoal
            )
        )
    }

    // -------------------------------------------------------------------------
    // Categories
    // -------------------------------------------------------------------------

    /**
     * Inserts the three default categories (Food, Transport, Entertainment)
     * if the categories table is empty. Called on first launch.
     */
    suspend fun ensureDefaultCategories() {
        if (categoryDao.count() == 0) {
            Log.d(TAG, "ensureDefaultCategories: seeding default categories")
            listOf("Food", "Transport", "Entertainment").forEach {
                categoryDao.insert(Category(name = it))
            }
        }
    }

    /** Returns all categories sorted by name. */
    suspend fun getCategories(): List<Category> {
        val list = categoryDao.getAll()
        Log.d(TAG, "getCategories: ${list.size} categories found")
        return list
    }

    /**
     * Adds a new category after normalising whitespace and checking for duplicates.
     * Throws IllegalArgumentException if the name is blank or already exists.
     */
    suspend fun addCategory(name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "Category name is required" }
        if (categoryDao.findByName(normalized) != null) {
            Log.e(TAG, "addCategory: duplicate category name '$normalized'")
            throw IllegalArgumentException("Category already exists")
        }
        val id = categoryDao.insert(Category(name = normalized))
        if (id == -1L) throw IllegalArgumentException("Category already exists")
        Log.d(TAG, "addCategory: inserted '$normalized' with id=$id")
    }

    /** Deletes a category from the database. */
    suspend fun deleteCategory(category: Category) {
        Log.d(TAG, "deleteCategory: removing '${category.name}' (id=${category.id})")
        categoryDao.delete(category)
    }

    // -------------------------------------------------------------------------
    // Expenses
    // -------------------------------------------------------------------------

    /**
     * Inserts a new expense, then triggers:
     *  1. Gamification check (streaks, badge unlocks, XP)
     *  2. Challenge progress update
     *  3. Budget badge evaluation
     *
     * Returns an UnlockResult containing any newly unlocked badges and XP earned.
     */
    suspend fun addExpense(
        userId: Int,
        amount: Double,
        date: String,
        categoryId: Int,
        note: String,
        receiptUri: String?
    ): GamificationManager.UnlockResult {
        Log.d(TAG, "addExpense: userId=$userId, amount=$amount, date=$date, categoryId=$categoryId")

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

        // Award XP and check for badge unlocks after every expense
        val unlockResult = gamificationManager.onExpenseLogged(userId, date)
        Log.d(TAG, "addExpense: gamification result -> badges=${unlockResult.newBadges.size}, xp=${unlockResult.totalXpEarned}")

        // Recalculate progress for any active challenges
        challengeRepository.updateChallengeProgress(userId)

        // Separately check budget-related badges (e.g. staying under budget)
        val goal = getBudgetGoal(userId)
        val monthlySpent = getMonthlySpent(userId)
        gamificationManager.checkBudgetBadges(userId, monthlySpent, goal.monthlyBudget)

        return unlockResult
    }

    /** Returns all expenses for a user, each joined with its category name. */
    suspend fun getAllExpenses(userId: Int): List<ExpenseWithCategory> {
        val list = expenseDao.getAllWithCategory(userId)
        Log.d(TAG, "getAllExpenses: userId=$userId -> ${list.size} expenses")
        return list
    }

    /** Returns the total of all expenses ever recorded for a user. */
    suspend fun getTotalSpent(userId: Int): Double = expenseDao.getTotalSpent(userId)

    /**
     * Calculates the total amount spent in the current calendar month
     * by filtering expenses whose date string contains the "MMM yyyy" pattern.
     */
    suspend fun getMonthlySpent(userId: Int): Double {
        val monthPattern = DateUtils.currentMonthPattern()
        val spent = expenseDao.getAllWithCategory(userId)
            .filter { it.date.contains(monthPattern) }
            .sumOf { it.amount }
        Log.d(TAG, "getMonthlySpent: userId=$userId, pattern=$monthPattern, spent=$spent")
        return spent
    }

    /** Fetches a single expense by its primary key, or null if not found. */
    suspend fun getExpenseById(id: Int): Expense? = expenseDao.getById(id)

    /** Attaches or updates the receipt image URI for an existing expense. */
    suspend fun updateReceipt(expenseId: Int, receiptUri: String?) {
        Log.d(TAG, "updateReceipt: expenseId=$expenseId, uri=$receiptUri")
        expenseDao.updateReceipt(expenseId, receiptUri)
    }

    /** Permanently removes an expense record by ID. */
    suspend fun deleteExpense(id: Int) {
        Log.d(TAG, "deleteExpense: removing expenseId=$id")
        expenseDao.deleteById(id)
    }

    // -------------------------------------------------------------------------
    // Analytics
    // -------------------------------------------------------------------------

    /**
     * Returns spending grouped by category for the given display-format date range.
     * The dates are converted to a sortable format internally for comparison.
     */
    suspend fun getSpendingByCategory(userId: Int, startDate: String, endDate: String) =
        filterAndAggregateByCategory(userId, startDate, endDate)

    /**
     * Returns spending grouped by day for the given display-format date range,
     * sorted chronologically.
     */
    suspend fun getDailySpending(userId: Int, startDate: String, endDate: String) =
        filterAndAggregateByDay(userId, startDate, endDate)

    private suspend fun filterAndAggregateByCategory(
        userId: Int,
        startDisplay: String,
        endDisplay: String
    ): List<com.budgetbuddy.data.dao.CategorySpending> {
        val start = DateUtils.toSortable(startDisplay)
        val end = DateUtils.toSortable(endDisplay)
        Log.d(TAG, "filterByCategory: userId=$userId, $start -> $end")
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
        Log.d(TAG, "filterByDay: userId=$userId, $start -> $end")
        return expenseDao.getAllWithCategory(userId)
            .filter { DateUtils.isInRange(it.date, start, end) }
            .groupBy { it.date }
            .map { (date, list) ->
                com.budgetbuddy.data.dao.DailySpending(date, list.sumOf { e -> e.amount })
            }
            .sortedBy { DateUtils.toSortable(it.date) }
    }
}
