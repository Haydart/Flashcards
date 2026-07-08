package com.rossomak.flashcards.feature.study.session

import androidx.lifecycle.SavedStateHandle
import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.FlashcardRating
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.CurationRepository
import com.rossomak.flashcards.core.domain.repository.FakeCurationRepository
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerConsentRepository
import com.rossomak.flashcards.core.domain.usecase.GetCurationRequestsUseCase
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.domain.usecase.ObserveVoiceAnswerConsentUseCase
import com.rossomak.flashcards.core.domain.usecase.SetVoiceAnswerConsentUseCase
import com.rossomak.flashcards.core.domain.usecase.ToggleCurationActionUseCase
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerState
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
import com.rossomak.flashcards.feature.study.voice.VoicePhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
    private val voiceAnswerConsentRepository = FakeVoiceAnswerConsentRepository()
    private val voiceGateway = FakeVoiceGateway()
    private val voiceSettingsController: VoiceSettingsController = mockk(relaxed = true)

    private val sessionTitle = "Compose"
    private val subcategoryId = "android-compose"

    private val route = StudySessionRoute(
        categoryId = "android",
        sessionTitle = sessionTitle,
        subcategoryIds = listOf(subcategoryId),
        cardIds = listOf("card-1", "card-2", "card-3"),
        studyMode = StudyMode.RATED,
    )

    @Before
    fun setUp() {
        mockkObject(RouteDecoder)
        stubRoute(route)
        every { voiceSettingsController.draftState } returns MutableStateFlow(VoiceSettingsDraftState())
        every { voiceSettingsController.currentSettings } returns VoiceSettings()
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
            GetCurationRequestsUseCase(curationRepository),
            ToggleCurationActionUseCase(curationRepository),
            ObserveVoiceAnswerConsentUseCase(voiceAnswerConsentRepository),
            SetVoiceAnswerConsentUseCase(voiceAnswerConsentRepository),
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
        stubRoute(route.copy(studyMode = StudyMode.FAST))
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
        viewModel.state.value.isSessionComplete shouldBe false
    }

    @Test
    fun `onRating advances to next card and hides the answer`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onShowAnswer()
        viewModel.onRating(FlashcardRating.CORRECT)

        viewModel.state.value.currentCardIndex shouldBe 1
        viewModel.state.value.isAnswerRevealed shouldBe false
    }

    @Test
    fun `onNextCard on the last card completes the session`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(listOf(flashcard("card-1")))
        stubRoute(route.copy(cardIds = listOf("card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onNextCard()

        viewModel.state.value.isSessionComplete shouldBe true
    }

    @Test
    fun `onVoiceAutoStartDeclined clears the pending flag`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(studyMode = StudyMode.FAST))
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onVoiceAutoStartDeclined()

        viewModel.state.value.isVoiceAutoStartPending shouldBe false
    }

    @Test
    fun `onVoiceAutoStart starts the gateway with loaded cards and applies saved settings`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedSettings = VoiceSettings(speechRate = 1.5f, voiceId = "voice-1")
        every { voiceSettingsController.currentSettings } returns savedSettings
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

        voiceGateway.stateFlow.value = VoicePlaybackState(isActive = true, currentIndex = 2, phase = VoicePhase.ANSWER)
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
    fun `onCurationFabClick loads the curation cache and shows the dialog`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val curationRepository = FakeCurationRepository()

        val viewModel = createViewModel(curationRepository)
        advanceUntilIdle()
        viewModel.onCurationFabClick()
        advanceUntilIdle()

        viewModel.state.value.isCurationDialogVisible shouldBe true
    }

    @Test
    fun `onCurationFabClick pauses playback when voice is playing`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        voiceGateway.stateFlow.value = VoicePlaybackState(isActive = true, isPlaying = true)
        advanceUntilIdle()

        viewModel.onCurationFabClick()
        advanceUntilIdle()

        voiceGateway.togglePlayPauseCalls shouldBe 1
    }

    @Test
    fun `onCurationActionToggle optimistically flags the current card and persists it`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val curationRepository = FakeCurationRepository()

        val viewModel = createViewModel(curationRepository)
        advanceUntilIdle()
        viewModel.onCurationActionToggle(CurationAction.DELETE)

        viewModel.state.value.curationRequests.getValue("card-1").actions.keys shouldBe setOf(CurationAction.DELETE)

        advanceUntilIdle()
        curationRepository.upsertedActions shouldBe listOf(Triple("card-1", subcategoryId, CurationAction.DELETE))
    }

    @Test
    fun `onCurationActionToggle replaces the opposite difficulty flag`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCurationActionToggle(CurationAction.DIFFICULTY_TOO_HARD)
        viewModel.onCurationActionToggle(CurationAction.DIFFICULTY_TOO_EASY)

        viewModel.state.value.curationRequests.getValue("card-1").actions.keys shouldBe
            setOf(CurationAction.DIFFICULTY_TOO_EASY)
    }

    @Test
    fun `onCurationActionToggle reverts optimistic state and surfaces error on failure`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val curationRepository: CurationRepository = mockk {
            coEvery { getCurationRequests(any()) } returns Result.success(emptyMap())
            coEvery { upsertCurationAction(any(), any(), any()) } returns Result.failure(IllegalStateException("boom"))
        }

        val viewModel = createViewModel(curationRepository)
        advanceUntilIdle()
        viewModel.onCurationActionToggle(CurationAction.DELETE)
        advanceUntilIdle()

        viewModel.state.value.curationRequests.containsKey("card-1") shouldBe false
        viewModel.state.value.curationError shouldBe "Failed to save curation request"
    }

    @Test
    fun `onCurationDialogDismiss hides the curation dialog`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCurationFabClick()
        advanceUntilIdle()

        viewModel.onCurationDialogDismiss()

        viewModel.state.value.isCurationDialogVisible shouldBe false
    }

    @Test
    fun `onCurationErrorDismissed clears the curation error`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val curationRepository: CurationRepository = mockk {
            coEvery { getCurationRequests(any()) } returns Result.success(emptyMap())
            coEvery { upsertCurationAction(any(), any(), any()) } returns Result.failure(IllegalStateException("boom"))
        }

        val viewModel = createViewModel(curationRepository)
        advanceUntilIdle()
        viewModel.onCurationActionToggle(CurationAction.DELETE)
        advanceUntilIdle()

        viewModel.onCurationErrorDismissed()

        viewModel.state.value.curationError shouldBe null
    }

    @Test
    fun `onCleared stops the voice gateway`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCleared()

        voiceGateway.stopCalls shouldBe 1
    }

    @Test
    fun `onVoiceAnswerToggle without consent shows the consent dialog even before the gateway is active`() = runTest(mainDispatcherRule.testDispatcher) {
        // Rated sessions never auto-start the gateway (ADR-0025) — the toggle must be reachable
        // while isVoiceActive is still false.
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceAnswerToggle()

        viewModel.state.value.isVoiceAnswerConsentDialogVisible shouldBe true
        voiceGateway.lastVoiceAnswering shouldBe null
    }

    @Test
    fun `onVoiceAnswerToggle with consent requests the mic permission even before the gateway is active`() = runTest(mainDispatcherRule.testDispatcher) {
        voiceAnswerConsentRepository.consentFlow.value = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceAnswerToggle()

        viewModel.state.value.isMicPermissionRequestPending shouldBe true
        viewModel.state.value.isVoiceAnswerConsentDialogVisible shouldBe false
    }

    @Test
    fun `onVoiceAnswerToggle in Fast mode does nothing`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(route.copy(studyMode = StudyMode.FAST))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceAnswerToggle()

        viewModel.state.value.isVoiceAnswerConsentDialogVisible shouldBe false
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
    fun `onVoiceAnswerConsentAccept persists consent and requests the mic permission`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceAnswerConsentAccept()
        advanceUntilIdle()

        voiceAnswerConsentRepository.consentFlow.value shouldBe true
        viewModel.state.value.isVoiceAnswerConsentDialogVisible shouldBe false
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

private class FakeVoiceAnswerConsentRepository : VoiceAnswerConsentRepository {
    val consentFlow = MutableStateFlow(false)

    override fun observeConsent(): Flow<Boolean> = consentFlow

    override suspend fun setConsent(granted: Boolean) {
        consentFlow.value = granted
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

    override fun stop() { stopCalls++ }
    override fun togglePlayPause() { togglePlayPauseCalls++ }
    override fun rewindToNext() { rewindToNextCalls++ }
    override fun rewindToPrevious() { rewindToPreviousCalls++ }
    override fun restartCurrentCard() { restartCurrentCardCalls++ }
    override fun showAnswer() { showAnswerCalls++ }
    override fun setSpeechRate(rate: Float) { lastSpeechRate = rate }
    override fun setVoice(voiceId: String?) { lastVoiceId = voiceId }
    override fun setVoiceAnswering(enabled: Boolean) { lastVoiceAnswering = enabled }
}
