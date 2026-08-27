package com.rossomak.flashcards.feature.settings

import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.model.VoiceSettings as SavedVoiceSettings
import com.rossomak.flashcards.core.domain.usecase.SignOutUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.feature.settings.SettingsDialog.Length
import com.rossomak.flashcards.feature.settings.SettingsDialog.Mode
import com.rossomak.flashcards.feature.settings.SettingsDialog.ReadAloud
import com.rossomak.flashcards.feature.settings.SettingsDialog.SignOut
import com.rossomak.flashcards.feature.settings.SettingsDialog.Sort
import com.rossomak.flashcards.feature.settings.SettingsDialog.VoiceSettings
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        signOutUseCase,
        voiceSettingsController,
    )

    @Test
    fun `confirming session length commits the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDialogEvent(Open(Length(draft = DEFAULT_LENGTH)))
        viewModel.onDialogEvent(DraftChange(Length(draft = LONGER_LENGTH)))
        viewModel.onDialogEvent(Confirm)

        viewModel.state.value.sessionLength shouldBe LONGER_LENGTH
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `dismissing session length discards the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val committedLength = viewModel.state.value.sessionLength

        viewModel.onDialogEvent(Open(Length(draft = committedLength)))
        viewModel.onDialogEvent(DraftChange(Length(draft = LONGER_LENGTH)))
        viewModel.onDialogEvent(Dismiss)

        viewModel.state.value.sessionLength shouldBe committedLength
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `confirming study mode commits the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDialogEvent(Open(Mode(draft = StudyMode.Rated)))
        viewModel.onDialogEvent(DraftChange(Mode(draft = StudyMode.Fast)))
        viewModel.onDialogEvent(Confirm)

        viewModel.state.value.defaultStudyMode shouldBe StudyMode.Fast
    }

    @Test
    fun `confirming sort order commits the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDialogEvent(Open(Sort(draft = FlashcardSortOrder.Default)))
        viewModel.onDialogEvent(DraftChange(Sort(draft = FlashcardSortOrder.EasiestFirst)))
        viewModel.onDialogEvent(Confirm)

        viewModel.state.value.sortOrder shouldBe FlashcardSortOrder.EasiestFirst
    }

    @Test
    fun `confirming read-aloud commits the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDialogEvent(Open(ReadAloud(draft = false)))
        viewModel.onDialogEvent(DraftChange(ReadAloud(draft = true)))
        viewModel.onDialogEvent(Confirm)

        viewModel.state.value.readAloudEnabled shouldBe true
    }

    @Test
    fun `editing the voice draft previews it`() = runTest(mainDispatcherRule.testDispatcher) {
        every { voiceSettingsController.seedDraft() } returns VoiceSettingsDraftState()
        val viewModel = createViewModel()
        val editedDraft = VoiceSettingsDraftState(draftSpeed = FASTER_SPEECH_RATE)

        viewModel.onDialogEvent(Open(VoiceSettings()))
        viewModel.onDialogEvent(DraftChange(VoiceSettings(draft = editedDraft)))

        verify(exactly = 1) { voiceSettingsController.preview(editedDraft) }
    }

    @Test
    fun `dismissing the voice dialog stops the preview`() = runTest(mainDispatcherRule.testDispatcher) {
        every { voiceSettingsController.seedDraft() } returns VoiceSettingsDraftState()
        val viewModel = createViewModel()

        viewModel.onDialogEvent(Open(VoiceSettings()))
        viewModel.onDialogEvent(Dismiss)

        verify(exactly = 1) { voiceSettingsController.stopPreview() }
    }

    @Test
    fun `dismissing a silent dialog leaves the shared player alone`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDialogEvent(Open(Sort(draft = FlashcardSortOrder.Default)))
        viewModel.onDialogEvent(Dismiss)

        verify(exactly = 0) { voiceSettingsController.stopPreview() }
    }

    @Test
    fun `the voice row summary follows the saved settings`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedVoice = VoiceOption(id = VOICE_ID, displayName = VOICE_DISPLAY_NAME)
        every { voiceSettingsController.loadVoices(any(), any()) } answers {
            secondArg<(List<VoiceOption>) -> Unit>().invoke(listOf(savedVoice))
        }
        every { voiceSettingsController.bind(any(), any()) } answers {
            secondArg<(SavedVoiceSettings) -> Unit>()
                .invoke(
                    SavedVoiceSettings(
                        speechRate = FASTER_SPEECH_RATE,
                        voiceId = VOICE_ID,
                    ),
                )
        }

        val viewModel = createViewModel()

        viewModel.state.value.voiceName shouldBe VOICE_DISPLAY_NAME
        viewModel.state.value.speechRate shouldBe FASTER_SPEECH_RATE
    }

    @Test
    fun `the voice row summary has no name until the voice list arrives`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { voiceSettingsController.bind(any(), any()) } answers {
                secondArg<(SavedVoiceSettings) -> Unit>()
                    .invoke(
                        SavedVoiceSettings(voiceId = VOICE_ID),
                    )
            }

            val viewModel = createViewModel()

            viewModel.state.value.voiceId shouldBe VOICE_ID
            viewModel.state.value.voiceName shouldBe null
        }

    @Test
    fun `confirming sign out signs out and emits Login`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } returns Unit

        val viewModel = createViewModel()
        viewModel.onDialogEvent(Open(SignOut))
        viewModel.onDialogEvent(Confirm)

        viewModel.events.test {
            awaitItem() shouldBe SettingsDestination.Login
        }
        viewModel.state.value.activeDialog shouldBe null
        coVerify(exactly = 1) { signOutUseCase() }
    }

    @Test
    fun `dismissing sign out does not sign out`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDialogEvent(Open(SignOut))
        viewModel.onDialogEvent(Dismiss)
        advanceUntilIdle()

        viewModel.state.value.activeDialog shouldBe null
        viewModel.state.value.isSigningOut shouldBe false
        coVerify(exactly = 0) { signOutUseCase() }
    }

    @Test
    fun `confirming sign out emits Login even when sign-out fails`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } throws RuntimeException("remote sign-out failed")

        val viewModel = createViewModel()
        viewModel.onDialogEvent(Open(SignOut))
        viewModel.onDialogEvent(Confirm)

        viewModel.events.test {
            awaitItem() shouldBe SettingsDestination.Login
        }
        coVerify(exactly = 1) { signOutUseCase() }
    }

    @Test
    fun `confirming sign out twice emits only one navigation event`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { signOutUseCase() } returns Unit

        val viewModel = createViewModel()
        viewModel.onDialogEvent(Open(SignOut))
        viewModel.onDialogEvent(Confirm)
        viewModel.onDialogEvent(Open(SignOut))
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        viewModel.events.test {
            awaitItem() shouldBe SettingsDestination.Login
            expectNoEvents()
        }
        coVerify(exactly = 1) { signOutUseCase() }
    }

    private companion object {
        const val DEFAULT_LENGTH = 20
        const val LONGER_LENGTH = 35
        const val FASTER_SPEECH_RATE = 1.25f
        const val VOICE_ID = "en-us-x-tpf-local"
        const val VOICE_DISPLAY_NAME = "English (United States) · en-us-x-tpf-local"
    }
}
