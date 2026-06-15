package com.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.data.entity.User
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: BudgetRepository) : ViewModel() {

    suspend fun login(email: String, password: String): User? = repository.login(email, password)

    suspend fun register(fullName: String, email: String, password: String): Result<User> =
        repository.registerUser(fullName, email, password)

    suspend fun resetPassword(email: String, newPassword: String): Boolean =
        repository.resetPassword(email, newPassword)
}
