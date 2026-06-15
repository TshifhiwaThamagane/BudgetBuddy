package com.budgetbuddy.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.data.ChallengeRepository
import com.budgetbuddy.data.entity.Challenge
import com.budgetbuddy.gamification.Badge
import com.budgetbuddy.gamification.GamificationManager
import kotlinx.coroutines.launch

class ChallengeViewModel(private val repository: BudgetRepository) : ViewModel() {

    data class ChallengeState(
        val activeChallenges: List<Challenge> = emptyList(),
        val completedChallenges: List<Challenge> = emptyList(),
        val templates: List<ChallengeRepository.ChallengeTemplate> = ChallengeRepository.AVAILABLE_CHALLENGES,
        val isLoading: Boolean = true,
        val message: String? = null,
        val error: String? = null
    )

    private val _state = MutableLiveData(ChallengeState())
    val state: LiveData<ChallengeState> = _state

    fun loadChallenges(userId: Int) {
        viewModelScope.launch {
            try {
                _state.value = _state.value?.copy(isLoading = true, error = null)
                val active = repository.challengeRepository.getActiveChallenges(userId)
                val completed = repository.challengeRepository.getCompletedChallenges(userId)
                _state.value = ChallengeState(
                    activeChallenges = active,
                    completedChallenges = completed,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value?.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun startChallenge(userId: Int, template: ChallengeRepository.ChallengeTemplate) {
        viewModelScope.launch {
            val result = repository.challengeRepository.startChallenge(userId, template)
            if (result == null) {
                _state.value = _state.value?.copy(message = "Challenge already active")
            } else {
                _state.value = _state.value?.copy(message = "Challenge started: ${template.title}")
            }
            loadChallenges(userId)
        }
    }
}

class BadgeGalleryViewModel(private val repository: BudgetRepository) : ViewModel() {

    data class BadgeGalleryState(
        val allBadges: List<Badge> = Badge.ALL_BADGES,
        val unlockedBadgeIds: Set<String> = emptySet(),
        val achievements: List<com.budgetbuddy.data.entity.Achievement> = emptyList(),
        val gamification: com.budgetbuddy.data.entity.UserGamification? = null,
        val isLoading: Boolean = true
    )

    private val _state = MutableLiveData(BadgeGalleryState())
    val state: LiveData<BadgeGalleryState> = _state

    fun load(userId: Int) {
        viewModelScope.launch {
            val unlocked = repository.gamificationManager.getUnlockedBadges(userId)
                .map { it.badgeId }.toSet()
            val achievements = repository.gamificationManager.getAchievements(userId)
            val gamification = repository.gamificationManager.getGamification(userId)
            _state.value = BadgeGalleryState(
                unlockedBadgeIds = unlocked,
                achievements = achievements,
                gamification = gamification,
                isLoading = false
            )
        }
    }
}
