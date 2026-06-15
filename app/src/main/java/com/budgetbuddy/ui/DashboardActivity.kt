package com.budgetbuddy.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.databinding.ActivityDashboardBinding
import com.budgetbuddy.ui.viewmodel.DashboardViewModel
import com.budgetbuddy.util.BudgetStatus
import com.budgetbuddy.util.CurrencyUtils
import com.budgetbuddy.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

/**
 * DashboardActivity — the main home screen shown after login.
 *
 * Displays:
 *  - Monthly budget, amount spent, remaining balance, and a colour-coded progress bar.
 *  - Gamification stats: XP level, daily/weekly/best streak.
 *  - AI-generated financial insights based on spending patterns.
 *  - Active budget challenges with live progress percentages.
 *
 * Architecture: MVVM — all data is observed via DashboardViewModel's LiveData.
 * The Activity itself only handles UI updates and user interaction.
 *
 * References:
 *  - ViewModel + LiveData: https://developer.android.com/topic/libraries/architecture/viewmodel
 *  - ViewBinding: https://developer.android.com/topic/libraries/view-binding
 *  - MaterialAlertDialogBuilder: https://material.io/components/dialogs/android
 */
class DashboardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DashboardActivity"
    }

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: Dashboard initialising")

        sessionManager = SessionManager(this)
        val repository = BudgetRepository.getInstance(this)

        // DashboardViewModelFactory injects the repository into the ViewModel
        viewModel = ViewModelProvider(
            this,
            DashboardViewModelFactory(repository)
        )[DashboardViewModel::class.java]

        // Personalise the greeting with the logged-in user's name
        binding.tvGreeting.text = "Hi, ${sessionManager.getUserName()}"
        Log.d(TAG, "Greeting set for user: ${sessionManager.getUserName()} (id=${sessionManager.getUserId()})")

        setupNavigation()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        // Reload dashboard every time the user returns (e.g. after adding an expense)
        Log.d(TAG, "onResume: reloading dashboard for userId=${sessionManager.getUserId()}")
        viewModel.loadDashboard(sessionManager.getUserId())
    }

    /**
     * Observes the single DashboardState object emitted by the ViewModel.
     * A single state object avoids multiple LiveData streams and makes UI updates predictable.
     */
    private fun observeState() {
        viewModel.state.observe(this) { state ->
            // Show or hide the loading spinner while data is being fetched
            binding.progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            if (state.isLoading) {
                Log.d(TAG, "State: loading...")
                return@observe
            }

            Log.d(TAG, "State received: spent=${state.monthlySpent}, budget=${state.monthlyBudget}, status=${state.status}")

            // --- Budget summary section ---
            binding.tvMonthlyBudgetValue.text = CurrencyUtils.toRand(state.monthlyBudget)
            binding.tvSpentValue.text = CurrencyUtils.toRand(state.monthlySpent)
            binding.tvRemainingValue.text = CurrencyUtils.toRand(state.remaining)
            binding.tvPercentageUsed.text = "${state.percentageUsed}%"
            binding.progressBudget.progress = state.percentageUsed
            binding.tvMinGoal.text = CurrencyUtils.toRand(state.minGoal)
            binding.tvMaxGoal.text = CurrencyUtils.toRand(state.maxGoal)

            // Colour-code the status badge based on BudgetStatus (SAFE / WARNING / DANGER)
            binding.tvStatusBadge.text = state.statusLabel
            val statusBg = when (state.status) {
                BudgetStatus.SAFE -> R.drawable.bg_status_safe
                BudgetStatus.WARNING -> R.drawable.bg_status_warning
                BudgetStatus.DANGER -> R.drawable.bg_status_danger
            }
            val statusColor = when (state.status) {
                BudgetStatus.SAFE -> R.color.status_safe
                BudgetStatus.WARNING -> R.color.status_warning
                BudgetStatus.DANGER -> R.color.status_danger
            }
            binding.tvStatusBadge.setBackgroundResource(statusBg)
            binding.tvStatusBadge.setTextColor(getColor(statusColor))
            // Tint the progress bar to match the status colour for visual consistency
            binding.progressBudget.progressTintList =
                android.content.res.ColorStateList.valueOf(getColor(statusColor))

            // --- Gamification section ---
            state.gamification?.let {
                Log.d(TAG, "Gamification: level=${it.level}, xp=${it.xp}")
                binding.tvLevelXp.text = "Level ${it.level} • ${it.xp} XP"
            }

            // --- Streak section ---
            state.streak?.let {
                Log.d(TAG, "Streaks: daily=${it.dailyStreak}, weekly=${it.weeklyStreak}, best=${it.bestStreak}")
                binding.tvDailyStreak.text = it.dailyStreak.toString()
                binding.tvWeeklyStreak.text = it.weeklyStreak.toString()
                binding.tvBestStreak.text = it.bestStreak.toString()
            }

            // --- Insights section ---
            if (state.insights.isNotEmpty()) {
                Log.d(TAG, "Displaying ${state.insights.size} financial insight(s)")
                binding.tvInsights.text = state.insights.joinToString("\n\n") {
                    "• ${it.title}: ${it.description}"
                }
            } else {
                binding.tvInsights.setText(R.string.no_insights)
            }

            // --- Active challenges section ---
            if (state.activeChallenges.isNotEmpty()) {
                Log.d(TAG, "Active challenges: ${state.activeChallenges.size}")
                binding.tvActiveChallenges.text = state.activeChallenges.joinToString("\n") {
                    "🏁 ${it.title} — ${it.currentProgress.toInt()}% complete"
                }
            } else {
                binding.tvActiveChallenges.setText(R.string.no_challenges)
            }

            // Display any error messages from the ViewModel (e.g. DB exceptions)
            state.error?.let {
                Log.e(TAG, "Dashboard error: $it")
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Wires up all navigation buttons to their corresponding Activities.
     */
    private fun setupNavigation() {
        binding.btnAddExpense.setOnClickListener {
            Log.d(TAG, "Navigating to AddExpenseActivity")
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        binding.btnCategories.setOnClickListener {
            Log.d(TAG, "Navigating to CategoryManagementActivity")
            startActivity(Intent(this, CategoryManagementActivity::class.java))
        }
        binding.btnExpenseList.setOnClickListener {
            Log.d(TAG, "Navigating to ExpenseListActivity")
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }
        binding.btnAnalytics.setOnClickListener {
            Log.d(TAG, "Navigating to AnalyticsActivity")
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
        binding.btnChallenges.setOnClickListener {
            Log.d(TAG, "Navigating to ChallengeActivity")
            startActivity(Intent(this, ChallengeActivity::class.java))
        }
        binding.btnBadges.setOnClickListener {
            Log.d(TAG, "Navigating to BadgeGalleryActivity")
            startActivity(Intent(this, BadgeGalleryActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            Log.d(TAG, "User logging out: id=${sessionManager.getUserId()}")
            sessionManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.btnEditBudget.setOnClickListener { showBudgetSettingsDialog() }
    }

    /**
     * Shows a Material dialog that lets the user update their monthly budget,
     * minimum savings goal, and maximum spending goal.
     * Pre-fills the fields with the current values from the ViewModel state.
     */
    private fun showBudgetSettingsDialog() {
        Log.d(TAG, "Opening budget settings dialog")
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget_settings, null)
        val etMonthly = dialogView.findViewById<TextInputEditText>(R.id.etMonthlyBudget)
        val etMin = dialogView.findViewById<TextInputEditText>(R.id.etMinGoal)
        val etMax = dialogView.findViewById<TextInputEditText>(R.id.etMaxGoal)

        // Pre-fill with existing values so the user can see and edit them easily
        val current = viewModel.state.value
        etMonthly.setText(current?.monthlyBudget?.toString() ?: "5000")
        etMin.setText(current?.minGoal?.toString() ?: "2000")
        etMax.setText(current?.maxGoal?.toString() ?: "4500")

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.budget_settings)
            .setView(dialogView)
            .setPositiveButton(R.string.save_settings) { _, _ ->
                val monthly = etMonthly.text.toString().toDoubleOrNull() ?: 5000.0
                val min = etMin.text.toString().toDoubleOrNull() ?: 2000.0
                val max = etMax.text.toString().toDoubleOrNull() ?: 4500.0
                Log.d(TAG, "Saving budget goal: monthly=$monthly, min=$min, max=$max")
                viewModel.saveBudgetGoal(sessionManager.getUserId(), monthly, min, max)
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}

/**
 * Factory required to pass the repository into DashboardViewModel's constructor.
 * ViewModelProvider cannot call non-default constructors without a factory.
 *
 * Reference: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
 */
class DashboardViewModelFactory(
    private val repository: BudgetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(repository) as T
    }
}
