package com.budgetbuddy.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.databinding.ActivityChallengeBinding
import com.budgetbuddy.ui.adapter.ChallengeAdapter
import com.budgetbuddy.ui.viewmodel.ChallengeViewModel
import com.budgetbuddy.util.SessionManager
import com.google.android.material.tabs.TabLayout

class ChallengeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChallengeBinding
    private lateinit var viewModel: ChallengeViewModel
    private lateinit var adapter: ChallengeAdapter
    private lateinit var sessionManager: SessionManager
    private var showingActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        val repository = BudgetRepository.getInstance(this)
        viewModel = ViewModelProvider(
            this,
            ChallengeViewModelFactory(repository)
        )[ChallengeViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ChallengeAdapter(
            onStartChallenge = { template ->
                viewModel.startChallenge(sessionManager.getUserId(), template)
            }
        )
        binding.recyclerChallenges.layoutManager = LinearLayoutManager(this)
        binding.recyclerChallenges.adapter = adapter

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.challenge_active))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.challenge_history))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showingActive = tab?.position == 0
                updateList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewModel.state.observe(this) { state ->
            updateList()
            state.message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadChallenges(sessionManager.getUserId())
    }

    private fun updateList() {
        val state = viewModel.state.value ?: return
        if (showingActive) {
            val items = state.activeChallenges.map { ChallengeAdapter.ChallengeItem.Active(it) } +
                state.templates.map { ChallengeAdapter.ChallengeItem.Template(it) }
            adapter.update(items)
            val isEmpty = items.isEmpty()
            binding.tvEmptyChallenges.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerChallenges.visibility = if (isEmpty) View.GONE else View.VISIBLE
        } else {
            val items = state.completedChallenges.map { ChallengeAdapter.ChallengeItem.Active(it) }
            adapter.update(items)
            val isEmpty = items.isEmpty()
            binding.tvEmptyChallenges.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerChallenges.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }
}

class ChallengeViewModelFactory(
    private val repository: BudgetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ChallengeViewModel(repository) as T
    }
}
