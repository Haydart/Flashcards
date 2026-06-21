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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
    ) = StudySessionViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "subcategoryId" to subcategoryId,
                "subcategoryName" to subcategoryName,
            )
        ),
        getFlashcards = getFlashcards,
        voiceGateway = fakeGateway,
    )

    private fun loadedViewModel() {
        coEvery { flashcardRepository.fetchFlashcards(any()) } returns Result.success(fakeFlashcards())
        viewModel.onCleared()
        fakeGateway = FakeVoiceGateway()
        viewModel = buildViewModel()
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
    fun `onCleared stops gateway`() {
        viewModel.onCleared()

        fakeGateway.stopCallCount shouldBe 1
    }
}

private class FakeVoiceGateway : VoiceGateway {
    private val _state = MutableStateFlow(VoicePlaybackState())
    override val state: StateFlow<VoicePlaybackState> = _state.asStateFlow()

    var startCallCount = 0
    var stopCallCount = 0
    var showAnswerCallCount = 0

    override fun start(
        cards: List<Flashcard>,
        startIndex: Int,
        subcategoryName: String,
    ) { startCallCount++ }

    override fun stop() { stopCallCount++ }
    override fun togglePlayPause() {}
    override fun skipNext() {}
    override fun skipPrevious() {}
    override fun showAnswer() { showAnswerCallCount++ }
    override fun setSpeechRate(rate: Float) {}

    fun emitState(state: VoicePlaybackState) { _state.value = state }
}

private fun fakeFlashcard(id: String = "1") = Flashcard(
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
    extendedContext = null,
)

private fun fakeFlashcards(count: Int = 5) = List(count) { fakeFlashcard(it.toString()) }
