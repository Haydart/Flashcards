package com.rossomak.flashcards.feature.study.session

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.FlashcardRating
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.CurationRepository
import com.rossomak.flashcards.core.domain.repository.FakeCurationRepository
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.repository.FakeUserPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.domain.usecase.ObserveUserPreferencesUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveUserPreferenceUseCase
import com.rossomak.flashcards.core.domain.usecase.SubmitCurationReportUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.session.StudySessionDialog.ExitSession
import com.rossomak.flashcards.feature.study.session.StudySessionDialog.ReportProblem
import com.rossomak.flashcards.feature.study.session.StudySessionDialog.VoiceAnswerConsent
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerState
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
import com.rossomak.flashcards.feature.study.voice.VoicePhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudySessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle: SavedStateHandle = mockk()
    private val flashcardRepository = FakeFlashcardRepository()
    private val getFlashcards = GetFlashcardsUseCase(flashcardRepository)
    private val userPreferencesRepository = FakeUserPreferencesRepository()
    private val voiceGateway = FakeVoiceGateway()
    private val voiceSettingsController: VoiceSettingsController = mockk(relaxed = true)

    private val sessionTitle = "Compose"
    private val subcategoryId = "android-compose"

    private val route = StudySessionRoute(
        categoryId = "android",
        sessionTitle = sessionTitle,
        subcategoryIds = listOf(subcategoryId),
        cardIds = listOf("card-1", "card-2", "card-3"),
        studyMode = StudyMode.Rated,
    )

    @Before
    fun setUp() {
        mockkObject(RouteDecoder)
        stubRoute(route)
    }

    @After
    fun tearDown() {
        unmockkObject(RouteDecoder)
    }

    private fun stubRoute(route: StudySessionRoute) {
        every { RouteDecoder.decode(any<() -> StudySessionRoute>()) } returns route
    }

    private fun createViewModel(curationRepository: CurationRepository = FakeCurationRepository()): StudySessionViewModel =
        StudySessionViewModel(
            savedStateHandle,
            getFlashcards,
            SubmitCurationReportUseCase(curationRepository),
            ObserveUserPreferencesUseCase(userPreferencesRepository),
            SaveUserPreferenceUseCase(userPreferencesRepository),
            voiceGateway,
            voiceSettingsController,
        )

    private fun flashcard(
        id: String,
        subcategoryId: String = this.subcategoryId,
        extendedContext: String? = null,
    ): Flashcard = Flashcard(
        id = id,
        subcategoryId = subcategoryId,
        tags = listOf("General"),
        question = "question-$id",
        answer = "answer-$id",
        difficulty = 5,
        questionCode = null,
        answerCode = null,
        questionSpoken = null,
        answerSpoken = null,
        extendedContext = extendedContext,
    )

    /** What the toolbar hands over: the report dialog seeded from the card on screen. */
    private fun openReportProblem(viewModel: StudySessionViewModel): ReportProblem {
        val card = requireNotNull(viewModel.state.value.currentCard)
        return ReportProblem(cardId = card.id, subcategoryId = card.subcategoryId)
    }

    private fun loadThreeCards() {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(
            listOf(flashcard("card-1"), flashcard("card-2"), flashcard("card-3")),
        )
    }

    @Test
    fun `loadFlashcards resolves routed card ids preserving order`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(
            listOf(flashcard("card-3"), flashcard("card-1"), flashcard("card-2")),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.flashcards.map { it.id } shouldBe route.cardIds
        viewModel.state.value.isLoading shouldBe false
        viewModel.state.value.isVoiceAutoStartPending shouldBe false
    }

    @Test
    fun `fast study mode marks voice auto start pending once cards load`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(studyMode = StudyMode.Fast))
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.isVoiceAutoStartPending shouldBe true
    }

    @Test
    fun `loadFlashcards surfaces error when any subcategory fetch fails`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.error shouldBe "Could not load flashcards"
        viewModel.state.value.isLoading shouldBe false
    }

    @Test
    fun `onShowAnswer reveals answer when voice inactive`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onShowAnswer()

        viewModel.state.value.isAnswerRevealed shouldBe true
        voiceGateway.showAnswerCalls shouldBe 0
    }

    @Test
    fun `onShowAnswer delegates to gateway when voice active`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        voiceGateway.stateFlow.value = VoicePlaybackState(isActive = true)
        advanceUntilIdle()

        viewModel.onShowAnswer()

        voiceGateway.showAnswerCalls shouldBe 1
    }

    @Test
    fun `onNextCard advances index and hides the answer`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onShowAnswer()
        viewModel.onNextCard()

        viewModel.state.value.currentCardIndex shouldBe 1
        viewModel.state.value.isAnswerRevealed shouldBe false
    }

    @Test
    fun `onRating advances to next card and hides the answer`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onShowAnswer()
        viewModel.onRating(FlashcardRating.Correct)

        viewModel.state.value.currentCardIndex shouldBe 1
        viewModel.state.value.isAnswerRevealed shouldBe false
    }

    @Test
    fun `onNextCard on the last card navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(listOf(flashcard("card-1")))
        stubRoute(route.copy(cardIds = listOf("card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onNextCard()

        viewModel.events.test { awaitItem() shouldBe StudySessionDestination.Back }
    }

    @Test
    fun `confirming the exit dialog closes it and navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(ExitSession))

        viewModel.onDialogEvent(Confirm)

        viewModel.state.value.activeDialog shouldBe null
        viewModel.events.test { awaitItem() shouldBe StudySessionDestination.Back }
    }

    @Test
    fun `dismissing the exit dialog closes it without navigating`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(ExitSession))

        viewModel.onDialogEvent(Dismiss)

        viewModel.state.value.activeDialog shouldBe null
        viewModel.events.test { expectNoEvents() }
    }

    @Test
    fun `onVoiceAutoStartDeclined clears the pending flag`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(studyMode = StudyMode.Fast))
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onVoiceAutoStartDeclined()

        viewModel.state.value.isVoiceAutoStartPending shouldBe false
    }

    @Test
    fun `onVoiceAutoStart starts the gateway with loaded cards and applies saved settings`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedSettings = VoiceSettings(speechRate = 1.5f, voiceId = "voice-1")
        stubRoute(route.copy(speechRate = savedSettings.speechRate, voiceId = savedSettings.voiceId))
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onVoiceAutoStart()
        advanceUntilIdle()

        voiceGateway.startCalls shouldBe 1
        voiceGateway.lastStartCards?.map { it.id } shouldBe route.cardIds
        voiceGateway.lastStartSubcategoryName shouldBe sessionTitle
        voiceGateway.lastSpeechRate shouldBe savedSettings.speechRate
        voiceGateway.lastVoiceId shouldBe savedSettings.voiceId
        viewModel.state.value.isVoiceAutoStartPending shouldBe false
    }

    @Test
    fun `onVoiceAutoStart ignores repeat calls while a session is already started`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onVoiceAutoStart()
        viewModel.onVoiceAutoStart()

        voiceGateway.startCalls shouldBe 1
    }

    @Test
    fun `observeVoiceState surfaces a voice error and clears active playback`() = runTest(mainDispatcherRule.testDispatcher) {
        val voiceError = "playback failed"
        val viewModel = createViewModel()
        advanceUntilIdle()

        voiceGateway.stateFlow.value = VoicePlaybackState(isActive = true, isPlaying = true, error = voiceError)
        advanceUntilIdle()

        viewModel.state.value.voiceError shouldBe voiceError
        viewModel.state.value.isVoiceActive shouldBe false
        viewModel.state.value.isVoicePlaying shouldBe false
    }

    @Test
    fun `observeVoiceState propagates active index and answer phase`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()

        voiceGateway.stateFlow.value = VoicePlaybackState(isActive = true, currentIndex = 2, phase = VoicePhase.Answer)
        advanceUntilIdle()

        viewModel.state.value.currentCardIndex shouldBe 2
        viewModel.state.value.isAnswerRevealed shouldBe true
        viewModel.state.value.isVoiceActive shouldBe true
    }

    @Test
    fun `onVoiceErrorDismissed clears the voice error`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        voiceGateway.stateFlow.value = VoicePlaybackState(error = "boom")
        advanceUntilIdle()

        viewModel.onVoiceErrorDismissed()

        viewModel.state.value.voiceError shouldBe null
    }

    @Test
    fun `onVoiceNext rewinds the gateway to the next card`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceNext()

        voiceGateway.rewindToNextCalls shouldBe 1
    }

    @Test
    fun `onVoicePlayPause toggles the gateway during normal playback`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoicePlayPause()

        voiceGateway.togglePlayPauseCalls shouldBe 1
    }

    @Test
    fun `onVoiceSpeedChange forwards the rate to the gateway`() = runTest(mainDispatcherRule.testDispatcher) {
        val rate = 1.75f
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceSpeedChange(rate)

        voiceGateway.lastSpeechRate shouldBe rate
    }

    @Test
    fun `ReportProblemOpen pauses playback when voice is playing`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        voiceGateway.stateFlow.value = VoicePlaybackState(isActive = true, isPlaying = true)
        advanceUntilIdle()

        viewModel.onDialogEvent(Open(openReportProblem(viewModel)))
        advanceUntilIdle()

        voiceGateway.togglePlayPauseCalls shouldBe 1
    }

    @Test
    fun `report draft is submittable only once an action is checked`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(openReportProblem(viewModel)))

        reportDraft(viewModel).canSubmit shouldBe false

        viewModel.onDialogEvent(
            DraftChange(
                reportDraft(viewModel).withAction(CurationAction.Delete, isChecked = true)
            )
        )

        reportDraft(viewModel).canSubmit shouldBe true
    }

    @Test
    fun `checking a difficulty action clears its opposite in the report draft`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(openReportProblem(viewModel)))

        viewModel.onDialogEvent(
            DraftChange(
                reportDraft(viewModel).withAction(CurationAction.DifficultyTooHard, isChecked = true)
            )
        )
        viewModel.onDialogEvent(
            DraftChange(
                reportDraft(viewModel).withAction(CurationAction.DifficultyTooEasy, isChecked = true)
            )
        )

        reportDraft(viewModel).selectedActions shouldBe setOf(CurationAction.DifficultyTooEasy)
    }

    @Test
    fun `unchecking an action removes it from the report draft`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(openReportProblem(viewModel)))

        viewModel.onDialogEvent(
            DraftChange(
                reportDraft(viewModel).withAction(CurationAction.WrongTags, isChecked = true)
            )
        )
        viewModel.onDialogEvent(
            DraftChange(
                reportDraft(viewModel).withAction(CurationAction.WrongTags, isChecked = false)
            )
        )

        reportDraft(viewModel).selectedActions shouldBe emptySet()
    }

    @Test
    fun `Confirm submits the whole checked set in one call and closes the dialog`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val curationRepository = FakeCurationRepository()
        val viewModel = createViewModel(curationRepository)
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(openReportProblem(viewModel)))
        viewModel.onDialogEvent(
            DraftChange(
                reportDraft(viewModel).withAction(CurationAction.Delete, isChecked = true)
            )
        )
        viewModel.onDialogEvent(
            DraftChange(
                reportDraft(viewModel).withAction(CurationAction.WrongTags, isChecked = true)
            )
        )

        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        curationRepository.submittedReports shouldBe listOf(
            Triple("card-1", subcategoryId, setOf(CurationAction.Delete, CurationAction.WrongTags))
        )
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `Dismiss discards the report draft without submitting`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val curationRepository = FakeCurationRepository()
        val viewModel = createViewModel(curationRepository)
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(openReportProblem(viewModel)))
        viewModel.onDialogEvent(
            DraftChange(
                reportDraft(viewModel).withAction(CurationAction.Delete, isChecked = true)
            )
        )

        viewModel.onDialogEvent(Dismiss)
        advanceUntilIdle()

        curationRepository.submittedReports shouldBe emptyList()
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `VoiceSettingsOpen seeds the draft from this session's current settings`() = runTest(mainDispatcherRule.testDispatcher) {
        val sessionSettings = VoiceSettings(speechRate = 1.5f, voiceId = "voice-1")
        stubRoute(route.copy(speechRate = sessionSettings.speechRate, voiceId = sessionSettings.voiceId))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDialogEvent(Open(StudySessionDialog.VoiceSettings()))

        viewModel.state.value.activeDialog.shouldBeInstanceOf<StudySessionDialog.VoiceSettings>()
        verify(exactly = 1) { voiceSettingsController.seedDraft(sessionSettings) }
    }

    @Test
    fun `VoiceSettings confirm without keepAsDefault applies for the session but writes nothing`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { voiceSettingsController.seedDraft(any()) } returns VoiceSettingsDraftState()
            val viewModel = createViewModel()
            advanceUntilIdle()
            voiceGateway.stateFlow.value = VoicePlaybackState(isActive = true)
            advanceUntilIdle()
            viewModel.onDialogEvent(Open(StudySessionDialog.VoiceSettings()))
            val draft = (viewModel.state.value.activeDialog as StudySessionDialog.VoiceSettings).draft
                .copy(draftSpeed = 1.5f, draftVoiceId = "voice-1")
            viewModel.onDialogEvent(DraftChange(StudySessionDialog.VoiceSettings(draft)))

            viewModel.onDialogEvent(Confirm)

            verify(exactly = 0) { voiceSettingsController.save(any(), any()) }
            verify(exactly = 1) { voiceSettingsController.stopPreview() }
            voiceGateway.lastSpeechRate shouldBe 1.5f
            voiceGateway.lastVoiceId shouldBe "voice-1"
            viewModel.state.value.activeDialog shouldBe null
        }

    @Test
    fun `VoiceSettings confirm with keepAsDefault writes the preference`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(StudySessionDialog.VoiceSettings()))
        val dialog = viewModel.state.value.activeDialog as StudySessionDialog.VoiceSettings
        viewModel.onDialogEvent(DraftChange(dialog.copy(keepAsDefault = true)))

        viewModel.onDialogEvent(Confirm)

        verify(exactly = 1) { voiceSettingsController.save(any(), any()) }
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `VoiceSettings Dismiss discards the draft through the controller`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(StudySessionDialog.VoiceSettings()))

        viewModel.onDialogEvent(Dismiss)

        verify(exactly = 1) { voiceSettingsController.stopPreview() }
        verify(exactly = 0) { voiceSettingsController.save(any(), any()) }
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `ExitSessionOpen shows the confirmation and Dismiss cancels it`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDialogEvent(Open(ExitSession))
        viewModel.state.value.activeDialog shouldBe ExitSession

        viewModel.onDialogEvent(Dismiss)
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `a routed voice-answering choice without consent opens the consent dialog on entry`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(route.copy(voiceAnsweringEnabled = true))
            loadThreeCards()

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.state.value.activeDialog shouldBe VoiceAnswerConsent
        }

    @Test
    fun `a routed voice-answering choice with consent requests the mic permission on entry`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(route.copy(voiceAnsweringEnabled = true))
            userPreferencesRepository.preferences.value = userPreferencesRepository.preferences.value.copy(voiceAnswerConsentGranted = true)
            loadThreeCards()

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.state.value.isMicPermissionRequestPending shouldBe true
        }

    @Test
    fun `a routed voice-answering choice is ignored in Fast mode`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(studyMode = StudyMode.Fast, voiceAnsweringEnabled = true))
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.activeDialog shouldBe null
        viewModel.state.value.isMicPermissionRequestPending shouldBe false
    }

    private fun reportDraft(viewModel: StudySessionViewModel): ReportProblem =
        viewModel.state.value.activeDialog as ReportProblem

    @Test
    fun `onCleared stops the voice gateway`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCleared()

        voiceGateway.stopCalls shouldBe 1
    }

    @Test
    fun `onVoiceAnswerToggle without consent shows the consent dialog even before the gateway is active`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Rated sessions never auto-start the gateway (ADR-0025) — the toggle must be reachable
            // while isVoiceActive is still false.
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onVoiceAnswerToggle()

            viewModel.state.value.activeDialog shouldBe VoiceAnswerConsent
            voiceGateway.lastVoiceAnswering shouldBe null
        }

    @Test
    fun `onVoiceAnswerToggle with consent requests the mic permission even before the gateway is active`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userPreferencesRepository.preferences.value = userPreferencesRepository.preferences.value.copy(voiceAnswerConsentGranted = true)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onVoiceAnswerToggle()

            viewModel.state.value.isMicPermissionRequestPending shouldBe true
            viewModel.state.value.activeDialog shouldBe null
        }

    @Test
    fun `onVoiceAnswerToggle in Fast mode does nothing`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(studyMode = StudyMode.Fast))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceAnswerToggle()

        viewModel.state.value.activeDialog shouldBe null
        viewModel.state.value.isMicPermissionRequestPending shouldBe false
    }

    @Test
    fun `onVoiceAnswerToggle while enabled stops the gateway`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        voiceGateway.voiceAnswerStateFlow.value = VoiceAnswerState(isEnabled = true)
        advanceUntilIdle()

        viewModel.onVoiceAnswerToggle()

        voiceGateway.stopCalls shouldBe 1
    }

    @Test
    fun `accepting voice-answer consent persists it and requests the mic permission`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onVoiceAnswerToggle()

        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        userPreferencesRepository.preferences.value.voiceAnswerConsentGranted shouldBe true
        viewModel.state.value.activeDialog shouldBe null
        viewModel.state.value.isMicPermissionRequestPending shouldBe true
    }

    @Test
    fun `onMicPermissionResult granted enables voice answering on the gateway`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMicPermissionResult(true)

        voiceGateway.lastVoiceAnswering shouldBe true
        viewModel.state.value.isMicPermissionRequestPending shouldBe false
    }

    @Test
    fun `onMicPermissionResult granted bootstraps the gateway in Rated mode`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMicPermissionResult(true)

        voiceGateway.startCalls shouldBe 1
        voiceGateway.lastStartCards?.map { it.id } shouldBe route.cardIds
        voiceGateway.lastVoiceAnswering shouldBe true
    }

    @Test
    fun `onMicPermissionResult denied leaves voice answering off`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMicPermissionResult(false)

        voiceGateway.lastVoiceAnswering shouldBe null
        viewModel.state.value.isMicPermissionRequestPending shouldBe false
    }

    @Test
    fun `voice answer state from the gateway is surfaced in screen state`() = runTest(mainDispatcherRule.testDispatcher) {
        val grade = VoiceAnswerGrade(sanitizedTranscript = "clean", gradePercent = 82, feedback = "good")
        val viewModel = createViewModel()
        advanceUntilIdle()

        voiceGateway.voiceAnswerStateFlow.value = VoiceAnswerState(isEnabled = true, lastGrade = grade)
        advanceUntilIdle()

        viewModel.state.value.isVoiceAnswerEnabled shouldBe true
        viewModel.state.value.lastVoiceAnswerGrade shouldBe grade
    }

    @Test
    fun `onVoiceAnswerGradeDismissed clears the last grade`() = runTest(mainDispatcherRule.testDispatcher) {
        val grade = VoiceAnswerGrade(sanitizedTranscript = "clean", gradePercent = 82, feedback = "good")
        val viewModel = createViewModel()
        advanceUntilIdle()
        voiceGateway.voiceAnswerStateFlow.value = VoiceAnswerState(isEnabled = true, lastGrade = grade)
        advanceUntilIdle()

        viewModel.onVoiceAnswerGradeDismissed()

        viewModel.state.value.lastVoiceAnswerGrade shouldBe null
    }
}

private class FakeVoiceGateway : VoiceGateway {
    val stateFlow = MutableStateFlow(VoicePlaybackState())
    override val state: StateFlow<VoicePlaybackState> = stateFlow

    val voiceAnswerStateFlow = MutableStateFlow(VoiceAnswerState())
    override val voiceAnswerState: StateFlow<VoiceAnswerState> = voiceAnswerStateFlow

    var lastVoiceAnswering: Boolean? = null

    var startCalls = 0
    var lastStartCards: List<Flashcard>? = null
    var lastStartIndex: Int? = null
    var lastStartSubcategoryName: String? = null
    var togglePlayPauseCalls = 0
    var rewindToNextCalls = 0
    var rewindToPreviousCalls = 0
    var restartCurrentCardCalls = 0
    var showAnswerCalls = 0
    var stopCalls = 0
    var lastSpeechRate: Float? = null
    var lastVoiceId: String? = null

    override fun start(cards: List<Flashcard>, startIndex: Int, subcategoryName: String) {
        startCalls++
        lastStartCards = cards
        lastStartIndex = startIndex
        lastStartSubcategoryName = subcategoryName
    }

    override fun stop() {
        stopCalls++
    }
    override fun togglePlayPause() {
        togglePlayPauseCalls++
    }
    override fun rewindToNext() {
        rewindToNextCalls++
    }
    override fun rewindToPrevious() {
        rewindToPreviousCalls++
    }
    override fun restartCurrentCard() {
        restartCurrentCardCalls++
    }
    override fun showAnswer() {
        showAnswerCalls++
    }
    override fun setSpeechRate(rate: Float) {
        lastSpeechRate = rate
    }
    override fun setVoice(voiceId: String?) {
        lastVoiceId = voiceId
    }
    override fun setVoiceAnswering(enabled: Boolean) {
        lastVoiceAnswering = enabled
    }
}
