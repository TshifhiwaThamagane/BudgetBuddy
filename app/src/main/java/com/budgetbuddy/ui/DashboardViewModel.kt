package com.budgetbuddy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.data.entity.BudgetGoal
import com.budgetbuddy.data.entity.Challenge
import com.budgetbuddy.data.entity.UserGamification
import com.budgetbuddy.data.entity.UserStreak
import com.budgetbuddy.insights.InsightsManager
import com.budgetbuddy.util.BudgetCalculator
import com.budgetbuddy.util.BudgetStatus
import com.budgetbuddy.util.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * DashboardViewModel — manages the state for DashboardActivity.
 *
 * Fetches all dashboard data from the repository, computes derived values
 * (remaining balance, budget percentage, status label), and exposes a single
 * [DashboardState] LiveData object to the UI layer.
 *
 * Using a single state object rather than multiple LiveData fields keeps the
 * UI update logic in one place and avoids partial-update flickering.
 *
 * References:
 *  - ViewModel overview: https://developer.android.com/topic/libraries/architecture/viewmodel
 *  - viewModelScope: https://developer.android.com/topic/libraries/architecture/coroutines#viewmodelscope
 */
class DashboardViewModel(private val repository: BudgetRepository) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    /**
     * Immutable snapshot of everything the Dashboard needs to render.
     * The UI observes this via LiveData and re-draws whenever it changes.
     */
    data class DashboardState(
        val monthlyBudget: Double = 5000.0,
        val minGoal: Double = 2000.0,
        val maxGoal: Double = 4500.0,
        val monthlySpent: Double = 0.0,
        val remaining: Double = 5000.0,
        val percentageUsed: Int = 0,
        val status: BudgetStatus = BudgetStatus.SAFE,
        val statusLabel: String = "Safe",
        val gamification: UserGamification? = null,
        val streak: UserStreak? = null,
        val insights: List<InsightsManager.FinancialInsight> = emptyList(),
        val activeChallenges: List<Challenge> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    // Backing MutableLiveData — only mutable inside the ViewModel
    private val _state = MutableLiveData(DashboardState())

    // Exposed as read-only LiveData to the Activity
    val state: LiveData<DashboardState> = _state

    /**
     * Loads all data needed for the dashboard in a single coroutine.
     * Sets [isLoading] to true at the start and false on completion.
     * Any exception is caught and surfaced via the [error] field.
     */
    fun loadDashboard(userId: Int) {
        Log.d(TAG, "loadDashboard: starting for userId=$userId")
        viewModelScope.launch {
            try {
                // Signal the UI to show a loading indicator
                _state.value = _state.value?.copy(isLoading = true, error = null)

                // Ensure categories and user gamification profile exist
                repository.ensureDefaultCategories()
                repository.ensureUserDefaults(userId)

                // Fetch all data concurrently within the same coroutine block
                val goal = repository.getBudgetGoal(userId)
                val spent = repository.getMonthlySpent(userId)

                // Derived calculations using the BudgetCalculator utility
                val remaining = BudgetCalculator.calculateRemaining(spent, goal.monthlyBudget)
                val percentage = BudgetCalculator.calculatePercentage(spent, goal.monthlyBudget)
                val status = BudgetCalculator.determineStatus(spent, goal.maxGoal, goal.monthlyBudget)

                val gamification = repository.gamificationManager.getGamification(userId)
                val streak = repository.gamificationManager.getStreak(userId)
                val insights = repository.insightsManager.generateInsights(userId, goal.monthlyBudget)
                val challenges = repository.challengeRepository.getActiveChallenges(userId)

                Log.d(TAG, "loadDashboard: spent=$spent/${goal.monthlyBudget}, $percentage%, status=$status")
                Log.d(TAG, "loadDashboard: ${insights.size} insights, ${challenges.size} active challenges")

                // Publish the complete state — Activity will re-draw
                _state.value = DashboardState(
                    monthlyBudget = goal.monthlyBudget,
                    minGoal = goal.minGoal,
                    maxGoal = goal.maxGoal,
                    monthlySpent = spent,
                    remaining = remaining,
                    percentageUsed = percentage,
                    status = status,
                    statusLabel = statusLabel(status),
                    gamification = gamification,
                    streak = streak,
                    insights = insights,
                    activeChallenges = challenges,
                    isLoading = false
                )
            } catch (e: Exception) {
                // Surface the error message to the UI without crashing
                Log.e(TAG, "loadDashboard: failed for userId=$userId", e)
                _state.value = _state.value?.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Persists updated budget goal settings, then reloads the dashboard
     * so all derived values (remaining, percentage, status) update immediately.
     */
    fun saveBudgetGoal(userId: Int, monthlyBudget: Double, minGoal: Double, maxGoal: Double) {
        Log.d(TAG, "saveBudgetGoal: userId=$userId, monthly=$monthlyBudget, min=$minGoal, max=$maxGoal")
        viewModelScope.launch {
            repository.saveBudgetGoal(userId, monthlyBudget, minGoal, maxGoal)
            loadDashboard(userId) // Refresh state after saving
        }
    }

    /**
     * Maps a [BudgetStatus] enum value to a human-readable string for display.
     */
    private fun statusLabel(status: BudgetStatus): String = when (status) {
        BudgetStatus.SAFE -> "Safe"
        BudgetStatus.WARNING -> "Near Budget"
        BudgetStatus.DANGER -> "Over Budget"
    }
}
