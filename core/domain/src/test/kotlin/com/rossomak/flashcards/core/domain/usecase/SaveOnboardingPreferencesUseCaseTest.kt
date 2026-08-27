package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.DailyGoal
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.repository.FakeStudySessionPreferencesRepository
import com.rossomak.flashcards.core.domain.repository.FakeUserPreferencesRepository
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveOnboardingPreferencesUseCaseTest {

    private val userPreferencesRepository = FakeUserPreferencesRepository()
    private val studySessionPreferencesRepository = FakeStudySessionPreferencesRepository()

    private val saveOnboardingPreferences = SaveOnboardingPreferencesUseCase(
        saveStudySessionPreference = SaveStudySessionPreferenceUseCase(studySessionPreferencesRepository),
        saveUserPreference = SaveUserPreferenceUseCase(userPreferencesRepository),
    )

    private companion object {
        const val LONGER_GOAL = DailyGoal.DEFAULT_MINUTES + DailyGoal.STEP_MINUTES
    }

    @Test
    fun `the happy path writes study mode, daily goal and the seen flag`() = runTest {
        val result = saveOnboardingPreferences(SaveOnboardingPreferencesUseCase.Params(StudyMode.Fast, LONGER_GOAL))

        result.isSuccess shouldBe true
        studySessionPreferencesRepository.preferences.value.defaultStudyMode shouldBe StudyMode.Fast
        userPreferencesRepository.preferences.value.dailyGoalMinutes shouldBe LONGER_GOAL
        userPreferencesRepository.preferences.value.hasSeenOnboarding shouldBe true
    }

    @Test
    fun `a failed study session preference write leaves onboarding incomplete`() = runTest {
        studySessionPreferencesRepository.saveError = IOException()

        val result = saveOnboardingPreferences(SaveOnboardingPreferencesUseCase.Params(StudyMode.Fast, LONGER_GOAL))

        result.isFailure shouldBe true
        userPreferencesRepository.preferences.value.hasSeenOnboarding shouldBe false
        userPreferencesRepository.preferences.value.dailyGoalMinutes shouldBe DailyGoal.DEFAULT_MINUTES
    }

    @Test
    fun `a failed daily goal write leaves onboarding incomplete`() = runTest {
        userPreferencesRepository.saveError = IOException()

        val result = saveOnboardingPreferences(SaveOnboardingPreferencesUseCase.Params(StudyMode.Fast, LONGER_GOAL))

        result.isFailure shouldBe true
        userPreferencesRepository.preferences.value.hasSeenOnboarding shouldBe false
        studySessionPreferencesRepository.preferences.value.defaultStudyMode shouldBe StudyMode.Fast
    }
}
