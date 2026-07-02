package com.rossomak.flashcards.feature.settings

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.usecase.SignOutUseCase
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    init {
        every { voiceSettingsController.draftState } returns MutableStateFlow(VoiceSettingsDraftState())
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(signOutUseCase, voiceSettingsController)

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
