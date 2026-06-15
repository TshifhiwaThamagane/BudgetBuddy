package com.budgetbuddy.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.databinding.ActivityBadgeGalleryBinding
import com.budgetbuddy.ui.adapter.AchievementAdapter
import com.budgetbuddy.ui.adapter.BadgeAdapter
import com.budgetbuddy.ui.viewmodel.BadgeGalleryViewModel
import com.budgetbuddy.util.SessionManager

class BadgeGalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBadgeGalleryBinding
    private lateinit var viewModel: BadgeGalleryViewModel
    private lateinit var badgeAdapter: BadgeAdapter
    private lateinit var achievementAdapter: AchievementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBadgeGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionManager = SessionManager(this)
        val repository = BudgetRepository.getInstance(this)
        viewModel = ViewModelProvider(
            this,
            BadgeGalleryViewModelFactory(repository)
        )[BadgeGalleryViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener { finish() }

        badgeAdapter = BadgeAdapter()
        achievementAdapter = AchievementAdapter()

        binding.recyclerBadges.layoutManager = LinearLayoutManager(this)
        binding.recyclerBadges.adapter = badgeAdapter
        binding.recyclerAchievements.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerAchievements.adapter = achievementAdapter

        viewModel.state.observe(this) { state ->
            state.gamification?.let {
                binding.tvLevelBadge.text = "Level ${it.level}"
                binding.tvXpBadge.text = "${it.xp} XP"
            }
            badgeAdapter.update(state.allBadges, state.unlockedBadgeIds)
            achievementAdapter.update(state.achievements)
        }

        viewModel.load(sessionManager.getUserId())
    }
}

class BadgeGalleryViewModelFactory(
    private val repository: BudgetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return BadgeGalleryViewModel(repository) as T
    }
}
