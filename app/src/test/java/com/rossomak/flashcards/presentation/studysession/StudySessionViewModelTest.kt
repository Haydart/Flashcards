package com.rossomak.flashcards.presentation.studysession

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.domain.model.Flashcard
import com.rossomak.flashcards.domain.repository.FlashcardRepository
import com.rossomak.flashcards.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.domain.voice.VoiceGateway
import com.rossomak.flashcards.domain.voice.VoicePhase
import com.rossomak.flashcards.domain.voice.VoicePlaybackState
import com.rossomak.flashcards.testutil.MainDispatcherRule
import com.rossomak.flashcards.testutil.assertValue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudySessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val flashcardRepository: FlashcardRepository = mockk()
    private val getFlashcards = GetFlashcardsUseCase(flashcardRepository)
    private lateinit var fakeGateway: FakeVoiceGateway
    private lateinit var viewModel: StudySessionViewModel

    @Before
    fun setUp() {
        fakeGateway = FakeVoiceGateway()
        coEvery { flashcardRepository.fetchFlashcards(any()) } returns Result.success(emptyList())
        viewModel = buildViewModel()
    }

    @After
    fun tearDown() {
        viewModel.onCleared()
    }

    private fun buildViewModel(
        subcategoryId: String = "sub-1",
        subcategoryName: String = "Kotlin",
        rewindThresholdMs: Long = Long.MAX_VALUE,
    ) = StudySessionViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "subcategoryId" to subcategoryId,
                "subcategoryName" to subcategoryName,
            )
        ),
        getFlashcards = getFlashcards,
        voiceGateway = fakeGateway,
    ).also { it.rewindThresholdMs = rewindThresholdMs }

    private fun loadedViewModel(
        rewindThresholdMs: Long = Long.MAX_VALUE,
        flashcards: List<Flashcard> = fakeFlashcards(),
    ) {
        coEvery { flashcardRepository.fetchFlashcards(any()) } returns Result.success(flashcards)
        viewModel.onCleared()
        fakeGateway = FakeVoiceGateway()
        viewModel = buildViewModel(rewindThresholdMs = rewindThresholdMs)
    }

    @Test
    fun `initial state has correct subcategoryName`() {
        viewModel.state.assertValue {
            subcategoryName shouldBe "Kotlin"
            isVoiceActive shouldBe false
            isLoading shouldBe false
        }
    }

    @Test
    fun `onToggleVoiceMode does nothing when flashcards empty`() {
        viewModel.onToggleVoiceMode()

        fakeGateway.startCallCount shouldBe 0
    }

    @Test
    fun `onToggleVoiceMode starts gateway when flashcards loaded`() {
        loadedViewModel()

        viewModel.onToggleVoiceMode()

        fakeGateway.startCallCount shouldBe 1
    }

    @Test
    fun `onToggleVoiceMode stops gateway on second call`() {
        loadedViewModel()

        viewModel.onToggleVoiceMode()
        viewModel.onToggleVoiceMode()

        fakeGateway.startCallCount shouldBe 1
        fakeGateway.stopCallCount shouldBe 1
    }

    @Test
    fun `voice state active propagates to screen state`() = runTest {
        viewModel.state.test {
            skipItems(1)
            fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, currentIndex = 2))
            awaitItem().run {
                isVoiceActive shouldBe true
                isVoicePlaying shouldBe true
                currentCardIndex shouldBe 2
            }
        }
    }

    @Test
    fun `voice answer phase reveals answer on screen`() = runTest {
        viewModel.state.test {
            skipItems(1)
            fakeGateway.emitState(VoicePlaybackState(isActive = true, phase = VoicePhase.ANSWER))
            awaitItem().isAnswerRevealed shouldBe true
        }
    }

    @Test
    fun `voice error clears active state and surfaces error`() = runTest {
        viewModel.state.test {
            skipItems(1)
            fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true))
            skipItems(1)
            fakeGateway.emitState(VoicePlaybackState(error = "tts_unavailable"))
            awaitItem().run {
                isVoiceActive shouldBe false
                isVoicePlaying shouldBe false
                voiceError shouldBe "tts_unavailable"
            }
        }
    }

    @Test
    fun `onShowAnswer delegates to gateway when voice active`() {
        fakeGateway.emitState(VoicePlaybackState(isActive = true))

        viewModel.onShowAnswer()

        fakeGateway.showAnswerCallCount shouldBe 1
        viewModel.state.value.isAnswerRevealed shouldBe false
    }

    @Test
    fun `onShowAnswer updates local state when voice inactive`() {
        viewModel.onShowAnswer()

        fakeGateway.showAnswerCallCount shouldBe 0
        viewModel.state.value.isAnswerRevealed shouldBe true
    }

    @Test
    fun `onVoiceErrorDismissed clears voiceError`() {
        fakeGateway.emitState(VoicePlaybackState(error = "tts_unavailable"))

        viewModel.onVoiceErrorDismissed()

        viewModel.state.value.voiceError shouldBe null
    }

    @Test
    fun `onVoicePrevious goes to previous card when below threshold`() {
        loadedViewModel() // rewindThresholdMs = Long.MAX_VALUE — delay never fires
        fakeGateway.emitState(VoicePlaybackState(isActive = true, currentIndex = 2))

        viewModel.onVoicePrevious()

        fakeGateway.rewindToPreviousCallCount shouldBe 1
        fakeGateway.restartCurrentCardCallCount shouldBe 0
    }

    @Test
    fun `onVoicePrevious restarts current card after threshold elapses`() {
        loadedViewModel(rewindThresholdMs = 0L) // delay(0) returns immediately — threshold fires synchronously
        fakeGateway.emitState(VoicePlaybackState(isActive = true, currentIndex = 2))

        viewModel.onVoicePrevious()

        fakeGateway.restartCurrentCardCallCount shouldBe 1
        fakeGateway.rewindToPreviousCallCount shouldBe 0
    }

    @Test
    fun `onVoicePrevious restarts current card when on first card`() {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, currentIndex = 0))

        viewModel.onVoicePrevious()

        fakeGateway.restartCurrentCardCallCount shouldBe 1
        fakeGateway.rewindToPreviousCallCount shouldBe 0
    }

    @Test
    fun `onExtendedContextDialogOpen while in between pause pauses playback`() {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = true, currentIndex = 0))

        viewModel.onExtendedContextDialogOpen()

        fakeGateway.togglePlayPauseCallCount shouldBe 1
    }

    @Test
    fun `onExtendedContextDialogOpen not in between pause does not pause`() {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = false, currentIndex = 0))

        viewModel.onExtendedContextDialogOpen()

        fakeGateway.togglePlayPauseCallCount shouldBe 0
    }

    @Test
    fun `entering between pause after dialog open pauses playback`() = runTest {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = false, currentIndex = 0))
        viewModel.onExtendedContextDialogOpen()

        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = true, currentIndex = 0))

        fakeGateway.togglePlayPauseCallCount shouldBe 1
    }

    @Test
    fun `onExtendedContextDialogDismissed before between pause prevents pause`() = runTest {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = false, currentIndex = 0))
        viewModel.onExtendedContextDialogOpen()
        viewModel.onExtendedContextDialogDismissed()

        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = true, currentIndex = 0))

        fakeGateway.togglePlayPauseCallCount shouldBe 0
    }

    @Test
    fun `onExtendedContextDialogOpen does nothing when voice inactive`() {
        viewModel.onExtendedContextDialogOpen()

        fakeGateway.togglePlayPauseCallCount shouldBe 0
    }

    @Test
    fun `onCleared stops gateway`() {
        viewModel.onCleared()

        fakeGateway.stopCallCount shouldBe 1
    }

    @Test
    fun `init failure sets error state`() {
        coEvery { flashcardRepository.fetchFlashcards(any()) } returns Result.failure(Exception("network"))
        viewModel.onCleared()
        viewModel = buildViewModel()

        viewModel.state.assertValue {
            error shouldBe "Could not load flashcards"
            isLoading shouldBe false
        }
    }

    @Test
    fun `onNextCard advances currentCardIndex and hides answer`() {
        loadedViewModel()
        viewModel.onShowAnswer()

        viewModel.onNextCard()

        viewModel.state.assertValue {
            currentCardIndex shouldBe 1
            isAnswerRevealed shouldBe false
        }
    }

    @Test
    fun `onNextCard on last card marks session complete`() {
        loadedViewModel(flashcards = fakeFlashcards(count = 1))

        viewModel.onNextCard()

        viewModel.state.value.isSessionComplete shouldBe true
    }

    @Test
    fun `onVoicePlayPause delegates to gateway`() {
        viewModel.onVoicePlayPause()

        fakeGateway.togglePlayPauseCallCount shouldBe 1
    }

    @Test
    fun `onVoicePlayPause when paused due to extended context rewinds to next and resumes`() {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = true, currentIndex = 0))
        viewModel.onExtendedContextDialogOpen()

        viewModel.onVoicePlayPause()

        fakeGateway.rewindToNextCallCount shouldBe 1
        fakeGateway.togglePlayPauseCallCount shouldBe 2
    }

    @Test
    fun `onVoiceNext calls rewindToNext on gateway`() {
        viewModel.onVoiceNext()

        fakeGateway.rewindToNextCallCount shouldBe 1
    }

    @Test
    fun `onVoiceNext clears pause state so subsequent play pause uses normal path`() {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = true, currentIndex = 0))
        viewModel.onExtendedContextDialogOpen()

        viewModel.onVoiceNext()
        viewModel.onVoicePlayPause()

        fakeGateway.rewindToNextCallCount shouldBe 1
        fakeGateway.togglePlayPauseCallCount shouldBe 2
    }

    @Test
    fun `onVoiceSpeedChange delegates to gateway`() {
        val rate = 1.5f

        viewModel.onVoiceSpeedChange(rate)

        fakeGateway.setSpeechRateCallCount shouldBe 1
        fakeGateway.lastSpeechRate shouldBe rate
    }

    @Test
    fun `voice speech rate propagates to screen state`() {
        fakeGateway.emitState(VoicePlaybackState(isActive = true, speechRate = 1.5f))

        viewModel.state.value.speechRate shouldBe 1.5f
    }

    @Test
    fun `voice becoming inactive clears voice active and playing state`() {
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true))

        fakeGateway.emitState(VoicePlaybackState(isActive = false))

        viewModel.state.assertValue {
            isVoiceActive shouldBe false
            isVoicePlaying shouldBe false
        }
    }

    @Test
    fun `card advance clears pause state so subsequent play pause uses normal path`() {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = true, currentIndex = 0))
        viewModel.onExtendedContextDialogOpen()

        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, currentIndex = 1))
        viewModel.onVoicePlayPause()

        fakeGateway.rewindToNextCallCount shouldBe 0
        fakeGateway.togglePlayPauseCallCount shouldBe 2
    }

    @Test
    fun `onExtendedContextDialogDismissed when paused due to extended context auto-advances after delay`() = runTest(mainDispatcherRule.testDispatcher) {
        loadedViewModel()
        fakeGateway.emitState(VoicePlaybackState(isActive = true, isPlaying = true, isInBetweenPause = true, currentIndex = 0))
        viewModel.onExtendedContextDialogOpen()

        viewModel.onExtendedContextDialogDismissed()
        advanceUntilIdle()

        fakeGateway.rewindToNextCallCount shouldBe 1
        fakeGateway.togglePlayPauseCallCount shouldBe 2
    }
}

private class FakeVoiceGateway : VoiceGateway {
    private val _state = MutableStateFlow(VoicePlaybackState())
    override val state: StateFlow<VoicePlaybackState> = _state.asStateFlow()

    var startCallCount = 0
    var stopCallCount = 0
    var showAnswerCallCount = 0
    var togglePlayPauseCallCount = 0
    var rewindToNextCallCount = 0
    var rewindToPreviousCallCount = 0
    var restartCurrentCardCallCount = 0
    var setSpeechRateCallCount = 0
    var lastSpeechRate: Float = 0f

    override fun start(
        cards: List<Flashcard>,
        startIndex: Int,
        subcategoryName: String,
    ) { startCallCount++ }

    override fun stop() { stopCallCount++ }
    override fun togglePlayPause() { togglePlayPauseCallCount++ }
    override fun rewindToNext() { rewindToNextCallCount++ }
    override fun rewindToPrevious() { rewindToPreviousCallCount++ }
    override fun restartCurrentCard() { restartCurrentCardCallCount++ }
    override fun showAnswer() { showAnswerCallCount++ }
    override fun setSpeechRate(rate: Float) {
        setSpeechRateCallCount++
        lastSpeechRate = rate
    }

    fun emitState(state: VoicePlaybackState) { _state.value = state }
}

private fun fakeFlashcard(id: String = "1", extendedContext: String? = "EC$id") = Flashcard(
    id = id,
    subcategoryId = "sub-1",
    tags = emptyList(),
    question = "Q$id",
    answer = "A$id",
    difficulty = 1,
    questionCode = null,
    answerCode = null,
    questionSpoken = null,
    answerSpoken = null,
    extendedContext = extendedContext,
)

private fun fakeFlashcards(count: Int = 5) = List(count) { fakeFlashcard(it.toString()) }
