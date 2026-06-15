package com.budgetbuddy.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.data.dao.CategorySpending
import com.budgetbuddy.data.dao.DailySpending
import com.budgetbuddy.data.entity.BudgetGoal
import com.budgetbuddy.util.DateUtils
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val repository: BudgetRepository) : ViewModel() {

    data class AnalyticsState(
        val categorySpending: List<CategorySpending> = emptyList(),
        val dailySpending: List<DailySpending> = emptyList(),
        val budgetGoal: BudgetGoal? = null,
        val startDate: String = DateUtils.toDisplay(DateUtils.startOfMonth()),
        val endDate: String = DateUtils.today(),
        val isEmpty: Boolean = true,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableLiveData(AnalyticsState())
    val state: LiveData<AnalyticsState> = _state

    fun loadAnalytics(userId: Int, startDisplay: String? = null, endDisplay: String? = null) {
        viewModelScope.launch {
            try {
                _state.value = _state.value?.copy(isLoading = true, error = null)
                val start = startDisplay ?: _state.value?.startDate ?: DateUtils.toDisplay(DateUtils.startOfMonth())
                val end = endDisplay ?: _state.value?.endDate ?: DateUtils.today()
                val startSortable = DateUtils.toSortable(start)
                val endSortable = DateUtils.toSortable(end)

                val categories = repository.getSpendingByCategory(userId, startSortable, endSortable)
                val daily = repository.getDailySpending(userId, startSortable, endSortable)
                val goal = repository.getBudgetGoal(userId)

                _state.value = AnalyticsState(
                    categorySpending = categories,
                    dailySpending = daily,
                    budgetGoal = goal,
                    startDate = start,
                    endDate = end,
                    isEmpty = categories.isEmpty() && daily.isEmpty(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value?.copy(isLoading = false, error = e.message)
            }
        }
    }
}
