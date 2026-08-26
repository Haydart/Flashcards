package com.rossomak.flashcards.feature.settings

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.repository.FakeUserPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.SetHasSeenOnboardingUseCase
import com.rossomak.flashcards.core.domain.usecase.SignOutUseCase
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signOutUseCase: SignOutUseCase = mockk()
    private val voiceSettingsController: VoiceSettingsController = mockk(relaxed = true)
    private val userPreferencesRepository = FakeUserPreferencesRepository()

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        signOutUseCase,
        SetHasSeenOnboardingUseCase(userPreferencesRepository),
        voiceSettingsController,
    )

    @Test
    fun `onReplayOnboardingClick clears the seen flag and emits Onboarding`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userPreferencesRepository.preferences.value =
                userPreferencesRepository.preferences.value.copy(hasSeenOnboarding = true)

            val viewModel = createViewModel()
            viewModel.onReplayOnboardingClick()

            viewModel.events.test {
                awaitItem() shouldBe SettingsDestination.Onboarding
            }
            userPreferencesRepository.preferences.value.hasSeenOnboarding shouldBe false
        }

    @Test
    fun `onSignOutClick with successful sign-out emits Login`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } returns Unit

        val viewModel = createViewModel()
        viewModel.onSignOutClick()

        viewModel.events.test {
            awaitItem() shouldBe SettingsDestination.Login
        }
        coVerify(exactly = 1) { signOutUseCase() }
    }

    @Test
    fun `onSignOutClick emits Login even when sign-out fails`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } throws RuntimeException("remote sign-out failed")

        val viewModel = createViewModel()
        viewModel.onSignOutClick()

        viewModel.events.test {
            awaitItem() shouldBe SettingsDestination.Login
        }
        coVerify(exactly = 1) { signOutUseCase() }
    }

    @Test
    fun `onSignOutClick while already signing out emits only one navigation event`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } returns Unit

        val viewModel = createViewModel()
        viewModel.onSignOutClick()
        viewModel.onSignOutClick()
        advanceUntilIdle()

        viewModel.events.test {
            awaitItem() shouldBe SettingsDestination.Login
            expectNoEvents()
        }
        coVerify(exactly = 1) { signOutUseCase() }
    }
}
