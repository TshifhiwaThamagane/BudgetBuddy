package com.budgetbuddy

import com.budgetbuddy.data.ChallengeRepository
import com.budgetbuddy.data.entity.Challenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeSystemTest {

    @Test
    fun `four challenge templates are available`() {
        assertEquals(4, ChallengeRepository.AVAILABLE_CHALLENGES.size)
    }

    @Test
    fun `save R500 challenge has correct target`() {
        val template = ChallengeRepository.AVAILABLE_CHALLENGES
            .find { it.type == ChallengeRepository.TYPE_SAVE_R500 }
        assertNotNull(template)
        assertEquals(500.0, template?.targetAmount ?: 0.0, 0.01)
    }

    @Test
    fun `spend less than R1000 challenge defined`() {
        val template = ChallengeRepository.AVAILABLE_CHALLENGES
            .find { it.type == ChallengeRepository.TYPE_SPEND_LESS_1000 }
        assertNotNull(template)
        assertEquals(1000.0, template?.targetAmount ?: 0.0, 0.01)
    }

    @Test
    fun `weekend budget challenge defined`() {
        val template = ChallengeRepository.AVAILABLE_CHALLENGES
            .find { it.type == ChallengeRepository.TYPE_WEEKEND_BUDGET }
        assertNotNull(template)
    }

    @Test
    fun `no fast food challenge has zero target`() {
        val template = ChallengeRepository.AVAILABLE_CHALLENGES
            .find { it.type == ChallengeRepository.TYPE_NO_FAST_FOOD }
        assertNotNull(template)
        assertEquals(0.0, template?.targetAmount ?: -1.0, 0.01)
    }

    @Test
    fun `challenge completion tracks progress`() {
        val challenge = Challenge(
            userId = 1,
            challengeType = ChallengeRepository.TYPE_SPEND_LESS_1000,
            title = "Spend Less Than R1000",
            description = "Test",
            targetAmount = 1000.0,
            currentProgress = 800.0,
            startDate = "01 Jun 2026",
            endDate = "30 Jun 2026",
            isCompleted = false,
            rewardPoints = 100
        )
        assertFalse(challenge.isCompleted)
        assertTrue(challenge.currentProgress < challenge.targetAmount)
    }

    private fun assertNotNull(value: Any?) {
        org.junit.Assert.assertNotNull(value)
    }
}
