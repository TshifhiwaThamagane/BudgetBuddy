package com.budgetbuddy.ui.viewmodel

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

class DashboardViewModel(private val repository: BudgetRepository) : ViewModel() {

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

    private val _state = MutableLiveData(DashboardState())
    val state: LiveData<DashboardState> = _state

    fun loadDashboard(userId: Int) {
        viewModelScope.launch {
            try {
                _state.value = _state.value?.copy(isLoading = true, error = null)
                repository.ensureDefaultCategories()
                repository.ensureUserDefaults(userId)

                val goal = repository.getBudgetGoal(userId)
                val spent = repository.getMonthlySpent(userId)
                val remaining = BudgetCalculator.calculateRemaining(spent, goal.monthlyBudget)
                val percentage = BudgetCalculator.calculatePercentage(spent, goal.monthlyBudget)
                val status = BudgetCalculator.determineStatus(spent, goal.maxGoal, goal.monthlyBudget)
                val gamification = repository.gamificationManager.getGamification(userId)
                val streak = repository.gamificationManager.getStreak(userId)
                val insights = repository.insightsManager.generateInsights(userId, goal.monthlyBudget)
                val challenges = repository.challengeRepository.getActiveChallenges(userId)

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
                _state.value = _state.value?.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun saveBudgetGoal(userId: Int, monthlyBudget: Double, minGoal: Double, maxGoal: Double) {
        viewModelScope.launch {
            repository.saveBudgetGoal(userId, monthlyBudget, minGoal, maxGoal)
            loadDashboard(userId)
        }
    }

    private fun statusLabel(status: BudgetStatus): String = when (status) {
        BudgetStatus.SAFE -> "Safe"
        BudgetStatus.WARNING -> "Near Budget"
        BudgetStatus.DANGER -> "Over Budget"
    }
}
