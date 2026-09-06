package com.rossomak.flashcards.feature.study.fast

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.CurationRepository
import com.rossomak.flashcards.core.domain.repository.FakeCurationRepository
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.domain.usecase.SubmitCurationReportUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.feature.study.FastStudySessionRoute
import com.rossomak.flashcards.feature.study.R
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.ExitSession
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.ReportProblem
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.VoiceSettings as VoiceSettingsDialog
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerState
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
import com.rossomak.flashcards.feature.study.voice.VoicePhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
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

/**
 * A Fast Study Session has no Ratings, no Attempts and no voice answering (ADR-0045) — this class
 * only ever exercises the surface [FastStudySessionViewModel] actually exposes, so a failure here
 * names Fast, never a Rated concept it does not share.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FastStudySessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle: SavedStateHandle = mockk()
    private val flashcardRepository = FakeFlashcardRepository()
    private val getFlashcards = GetFlashcardsUseCase(flashcardRepository)
    private val voiceGateway = FakeVoiceGateway()
    private val voiceSettingsController: VoiceSettingsController = mockk(relaxed = true)

    private val sessionTitle = "Compose"
    private val subcategoryId = "android-compose"

    private val route = FastStudySessionRoute(
        categoryId = "android",
        sessionTitle = sessionTitle,
        subcategoryIds = listOf(subcategoryId),
        cardIds = listOf("card-1", "card-2", "card-3"),
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

    private fun stubRoute(route: FastStudySessionRoute) {
        every { RouteDecoder.decode(any<() -> FastStudySessionRoute>()) } returns route
    }

    private fun createViewModel(curationRepository: CurationRepository = FakeCurationRepository()): FastStudySessionViewModel =
        FastStudySessionViewModel(
            savedStateHandle,
            getFlashcards,
            SubmitCurationReportUseCase(curationRepository),
            voiceGateway,
            voiceSettingsController,
        )

    private fun flashcard(id: String, subcategoryId: String = this.subcategoryId): Flashcard = Flashcard(
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
        extendedContext = null,
    )

    private fun openReportProblem(viewModel: FastStudySessionViewModel): ReportProblem {
        val card = requireNotNull(viewModel.state.value.currentCard)
        return ReportProblem(cardId = card.id, subcategoryId = card.subcategoryId)
    }

    private fun loadThreeCards() {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(
            listOf(flashcard("card-1"), flashcard("card-2"), flashcard("card-3")),
        )
    }

    private fun reportDraft(viewModel: FastStudySessionViewModel): ReportProblem =
        viewModel.state.value.activeDialog as ReportProblem

    @Test
    fun `loadFlashcards resolves routed card ids preserving order`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(
            listOf(flashcard("card-3"), flashcard("card-1"), flashcard("card-2")),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.flashcards.map { it.id } shouldBe route.cardIds
        viewModel.state.value.isLoading shouldBe false
    }

    @Test
    fun `loadFlashcards surfaces error when any subcategory fetch fails`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.error shouldBe R.string.study_session_load_error_message
        viewModel.state.value.isLoading shouldBe false
    }

    @Test
    fun `read-aloud off never marks voice auto start pending`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(readAloudEnabled = false))
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.isVoiceAutoStartPending shouldBe false
    }

    @Test
    fun `read-aloud on marks voice auto start pending once cards load`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(readAloudEnabled = true))
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.isVoiceAutoStartPending shouldBe true
    }

    @Test
    fun `read-aloud off with reveal-then-Next advances the deck manually`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(readAloudEnabled = false))
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onShowAnswer()
        viewModel.onNextCard()

        viewModel.state.value.isVoiceAutoStartPending shouldBe false
        voiceGateway.startCalls shouldBe 0
        viewModel.state.value.currentCardIndex shouldBe 1
        viewModel.state.value.isAnswerRevealed shouldBe false
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
    fun `onNextCard on the last card navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(listOf(flashcard("card-1")))
        stubRoute(route.copy(cardIds = listOf("card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onNextCard()

        viewModel.events.test { awaitItem() shouldBe FastStudySessionDestination.Back }
    }

    @Test
    fun `confirming the exit dialog closes it and navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(ExitSession))

        viewModel.onDialogEvent(Confirm)

        viewModel.state.value.activeDialog shouldBe null
        viewModel.events.test { awaitItem() shouldBe FastStudySessionDestination.Back }
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
        stubRoute(route.copy(readAloudEnabled = true))
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onVoiceAutoStartDeclined()

        viewModel.state.value.isVoiceAutoStartPending shouldBe false
    }

    @Test
    fun `onVoiceAutoStart starts the gateway with loaded cards and applies saved settings`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedSettings = VoiceSettings(speechRate = 1.5f, voiceId = "voice-1")
        stubRoute(route.copy(readAloudEnabled = true, speechRate = savedSettings.speechRate, voiceId = savedSettings.voiceId))
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
        val draft = viewModel.state.value.activeDialog as ReportProblem
        viewModel.onDialogEvent(DraftChange(draft.withAction(CurationAction.Delete, isChecked = true)))

        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        curationRepository.submittedReports shouldBe listOf(
            Triple("card-1", subcategoryId, setOf(CurationAction.Delete))
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

        viewModel.onDialogEvent(Open(VoiceSettingsDialog()))

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
            viewModel.onDialogEvent(Open(VoiceSettingsDialog()))
            val draft = (viewModel.state.value.activeDialog as VoiceSettingsDialog).draft
                .copy(draftSpeed = 1.5f, draftVoiceId = "voice-1")
            viewModel.onDialogEvent(DraftChange(VoiceSettingsDialog(draft)))

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
        viewModel.onDialogEvent(Open(VoiceSettingsDialog()))
        val dialog = viewModel.state.value.activeDialog as VoiceSettingsDialog
        viewModel.onDialogEvent(DraftChange(dialog.copy(keepAsDefault = true)))

        viewModel.onDialogEvent(Confirm)

        verify(exactly = 1) { voiceSettingsController.save(any(), any()) }
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `VoiceSettings Dismiss discards the draft through the controller`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(VoiceSettingsDialog()))

        viewModel.onDialogEvent(Dismiss)

        verify(exactly = 1) { voiceSettingsController.stopPreview() }
        verify(exactly = 0) { voiceSettingsController.save(any(), any()) }
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `onCleared stops the voice gateway`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCleared()

        voiceGateway.stopCalls shouldBe 1
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
