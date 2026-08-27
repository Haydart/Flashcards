package com.rossomak.flashcards.feature.debug

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.repository.FakeUserPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.SaveUserPreferenceUseCase
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebugViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userPreferencesRepository = FakeUserPreferencesRepository()

    private fun createViewModel(): DebugViewModel =
        DebugViewModel(SaveUserPreferenceUseCase(userPreferencesRepository))

    @Test
    fun `onReplayOnboardingClick clears the seen flag and emits Onboarding`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userPreferencesRepository.preferences.value =
                userPreferencesRepository.preferences.value.copy(hasSeenOnboarding = true)

            val viewModel = createViewModel()
            viewModel.onReplayOnboardingClick()

            viewModel.events.test {
                awaitItem() shouldBe DebugDestination.Onboarding
            }
            userPreferencesRepository.preferences.value.hasSeenOnboarding shouldBe false
        }
}
