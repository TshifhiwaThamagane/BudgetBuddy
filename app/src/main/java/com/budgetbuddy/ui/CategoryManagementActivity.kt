package com.budgetbuddy.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.ui.adapter.CategoryAdapter
import kotlinx.coroutines.launch

class CategoryManagementActivity : AppCompatActivity() {
    private lateinit var repository: BudgetRepository
    private lateinit var adapter: CategoryAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_management)

        repository = BudgetRepository.getInstance(this)

        val etCategory = findViewById<EditText>(R.id.etCategoryName)
        val btnAdd = findViewById<Button>(R.id.btnAddCategory)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerCategories)
        tvEmpty = findViewById(R.id.tvEmptyCategories)

        adapter = CategoryAdapter(mutableListOf()) { category ->
            lifecycleScope.launch {
                repository.deleteCategory(category)
                loadCategories()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            val name = etCategory.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    repository.addCategory(name)
                    etCategory.text.clear()
                    loadCategories()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@CategoryManagementActivity,
                        e.message ?: "Could not add category",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            repository.ensureDefaultCategories()
            loadCategories()
        }
    }

    private suspend fun loadCategories() {
        val categories = repository.getCategories()
        adapter.update(categories)
        tvEmpty.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
    }
}
