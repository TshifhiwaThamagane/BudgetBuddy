package com.budgetbuddy.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.databinding.ActivityExpenseListBinding
import com.budgetbuddy.ui.adapter.ExpenseAdapter
import com.budgetbuddy.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ExpenseListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseListBinding
    private lateinit var repository: BudgetRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BudgetRepository.getInstance(this)
        sessionManager = SessionManager(this)

        adapter = ExpenseAdapter(mutableListOf()) { expense ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_expense)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes) { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteExpense(expense.id)
                        loadExpenses()
                        Toast.makeText(this@ExpenseListActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        binding.recyclerExpenses.layoutManager = LinearLayoutManager(this)
        binding.recyclerExpenses.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val items = repository.getAllExpenses(sessionManager.getUserId())
            adapter.update(items)
            binding.tvEmptyExpenses.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
