package com.rossomak.flashcards.feature.study.rated

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.FlashcardRating
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
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
import com.rossomak.flashcards.core.ui.composables.FlashcardsAttemptSlotState
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.feature.study.RatedStudySessionRoute
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.ExitSession
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.ReportProblem
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.VoiceAnswerConsent
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerPhase
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerState
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
import com.rossomak.flashcards.feature.study.voice.VoicePhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * A Rated Study Session owns Ratings and voice answering; it has no Read-aloud auto-start and no
 * notification-permission path — those are Fast concepts (ADR-0045).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RatedStudySessionViewModelTest {

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

    private val route = RatedStudySessionRoute(
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

    private fun stubRoute(route: RatedStudySessionRoute) {
        every { RouteDecoder.decode(any<() -> RatedStudySessionRoute>()) } returns route
    }

    private fun createViewModel(curationRepository: CurationRepository = FakeCurationRepository()): RatedStudySessionViewModel =
        RatedStudySessionViewModel(
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
    private fun openReportProblem(viewModel: RatedStudySessionViewModel): ReportProblem {
        val card = requireNotNull(viewModel.state.value.currentCard)
        return ReportProblem(cardId = card.id, subcategoryId = card.subcategoryId)
    }

    private fun loadThreeCards() {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(
            listOf(flashcard("card-1"), flashcard("card-2"), flashcard("card-3")),
        )
    }

    /** A pool large enough that a re-insertion gap lands mid-queue instead of clamping to the end. */
    private fun loadTenCards() {
        val cardIds = (1..10).map { "card-$it" }
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(cardIds.map { flashcard(it) })
        stubRoute(route.copy(cardIds = cardIds))
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
    fun `rating the current card Correct removes it and advances to the next`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onShowAnswer()
        viewModel.onRating(FlashcardRating.Correct)

        viewModel.state.value.currentCard?.id shouldBe "card-2"
        viewModel.state.value.isAnswerRevealed shouldBe false
    }

    @Test
    fun `onRating on the last card navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(listOf(flashcard("card-1")))
        stubRoute(route.copy(cardIds = listOf("card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onRating(FlashcardRating.Correct)

        viewModel.events.test { awaitItem() shouldBe RatedStudySessionDestination.Back }
    }

    @Test
    fun `rating it Failed keeps it in the session and brings it back later`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRating(FlashcardRating.Failed)

        viewModel.state.value.currentCard?.id shouldNotBe "card-1"
        viewModel.state.value.flashcards.map { it.id } shouldContain "card-1"
    }

    @Test
    fun `the mastered count increases only on a Terminal Mastered`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRating(FlashcardRating.Failed)
        viewModel.state.value.masteredCount shouldBe 0

        viewModel.onRating(FlashcardRating.Correct)
        viewModel.state.value.masteredCount shouldBe 1
    }

    @Test
    fun `the mastered count does not move on a card finishing Partial or Failed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(
                listOf(flashcard("card-1"), flashcard("card-2")),
            )
            stubRoute(route.copy(cardIds = listOf("card-1", "card-2"), ratedAttempts = 1))
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRating(FlashcardRating.Failed)
            viewModel.state.value.masteredCount shouldBe 0

            viewModel.onRating(FlashcardRating.PartiallyCorrect)
            viewModel.state.value.masteredCount shouldBe 0
        }

    @Test
    fun `the distinct card total is fixed at session start and does not grow as the queue grows`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadThreeCards()
            val viewModel = createViewModel()
            advanceUntilIdle()
            val distinctCountBefore = viewModel.state.value.distinctCardCount

            viewModel.onRating(FlashcardRating.Failed)

            viewModel.state.value.distinctCardCount shouldBe distinctCountBefore
            viewModel.state.value.distinctCardCount shouldBe 3
        }

    @Test
    fun `the attempt indicator's slots reflect the current card's Rating list in order`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadTenCards()
            val viewModel = createViewModel().apply { random = Random(FIXED_SEED) }
            advanceUntilIdle()

            viewModel.onRating(FlashcardRating.Failed)
            // The card that just went to the back of the queue is not the head any more, so cycle
            // through the others (Correct finishes them immediately) until it resurfaces.
            while (viewModel.state.value.currentCard?.id != "card-1") {
                viewModel.onRating(FlashcardRating.Correct)
            }

            viewModel.state.value.currentCardRatings shouldBe listOf(FlashcardRating.Failed)
            viewModel.state.value.attemptSlots shouldBe listOf(
                FlashcardsAttemptSlotState.Failed,
                FlashcardsAttemptSlotState.Current,
                FlashcardsAttemptSlotState.Future,
            )
        }

    @Test
    fun `the attempt indicator's Current position and Future count match the configured Attempts limit`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadThreeCards()
            stubRoute(route.copy(cardIds = listOf("card-1", "card-2", "card-3"), ratedAttempts = 4))
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.state.value.attemptSlots shouldBe listOf(
                FlashcardsAttemptSlotState.Current,
                FlashcardsAttemptSlotState.Future,
                FlashcardsAttemptSlotState.Future,
                FlashcardsAttemptSlotState.Future,
            )
        }

    @Test
    fun `the terminal navigation event fires exactly once, when the last card finishes`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadThreeCards()
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onRating(FlashcardRating.Correct)
            viewModel.onRating(FlashcardRating.Correct)

            viewModel.events.test {
                viewModel.onRating(FlashcardRating.Correct)
                awaitItem() shouldBe RatedStudySessionDestination.Back
            }
        }

    @Test
    fun `partialRatingCardRequeueingEnabled false ends a Partial rating immediately as Terminal Partial`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(listOf(flashcard("card-1")))
            stubRoute(route.copy(cardIds = listOf("card-1"), partialRatingCardRequeueingEnabled = false))
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRating(FlashcardRating.PartiallyCorrect)

            viewModel.events.test { awaitItem() shouldBe RatedStudySessionDestination.Back }
        }

    @Test
    fun `partialRatingCardRequeueingEnabled true re-queues a Partial rating`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRating(FlashcardRating.PartiallyCorrect)

        viewModel.state.value.currentCard?.id shouldNotBe "card-1"
        viewModel.state.value.flashcards.map { it.id } shouldContain "card-1"
    }

    @Test
    fun `a rating sequence produces the expected order of displayed cards under a fixed Random`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadTenCards()
            val ratingSequence = listOf(FlashcardRating.Failed, FlashcardRating.PartiallyCorrect, FlashcardRating.Failed)

            val firstViewModel = createViewModel().apply { random = Random(FIXED_SEED) }
            advanceUntilIdle()
            ratingSequence.forEach(firstViewModel::onRating)
            val firstOrder = firstViewModel.state.value.flashcards.map { it.id }

            val secondViewModel = createViewModel().apply { random = Random(FIXED_SEED) }
            advanceUntilIdle()
            ratingSequence.forEach(secondViewModel::onRating)
            val secondOrder = secondViewModel.state.value.flashcards.map { it.id }

            firstOrder shouldBe secondOrder
        }

    @Test
    fun `confirming the exit dialog closes it and navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(ExitSession))

        viewModel.onDialogEvent(Confirm)

        viewModel.state.value.activeDialog shouldBe null
        viewModel.events.test { awaitItem() shouldBe RatedStudySessionDestination.Back }
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
            val draft = (viewModel.state.value.activeDialog as StudySessionDialog.VoiceSettings).draftState
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

    private fun reportDraft(viewModel: RatedStudySessionViewModel): ReportProblem =
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
    fun `a failed consent save keeps the dialog open, surfaces an error, and skips the mic request`() =
        runTest(mainDispatcherRule.testDispatcher) {
            userPreferencesRepository.saveError = IllegalStateException("disk full")
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onVoiceAnswerToggle()

            viewModel.onDialogEvent(Confirm)
            advanceUntilIdle()

            userPreferencesRepository.preferences.value.voiceAnswerConsentGranted shouldBe false
            viewModel.state.value.activeDialog shouldBe VoiceAnswerConsent
            viewModel.state.value.isMicPermissionRequestPending shouldBe false
            viewModel.state.value.voiceError shouldBe "Failed to save voice answering consent"
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
    fun `onMicPermissionResult granted bootstraps the gateway`() = runTest(mainDispatcherRule.testDispatcher) {
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

    /**
     * Two [MutableStateFlow] writes, each followed by [advanceUntilIdle], so the collector actually
     * observes the intermediate phase — writing SpeakingNotice twice in a row without that would
     * conflate into one emission (equal consecutive [VoiceAnswerState] values), silently dropping a
     * silence timeout.
     */
    private fun TestScope.emitSilenceTimeout() {
        voiceGateway.voiceAnswerStateFlow.value = VoiceAnswerState(isEnabled = true, phase = VoiceAnswerPhase.Listening)
        advanceUntilIdle()
        voiceGateway.voiceAnswerStateFlow.value = VoiceAnswerState(isEnabled = true, phase = VoiceAnswerPhase.SpeakingNotice)
        advanceUntilIdle()
    }

    private fun TestScope.emitGrade(grade: VoiceAnswerGrade, cardId: String) {
        voiceGateway.voiceAnswerStateFlow.value = VoiceAnswerState(isEnabled = true, phase = VoiceAnswerPhase.Grading)
        advanceUntilIdle()
        voiceGateway.voiceAnswerStateFlow.value = VoiceAnswerState(
            isEnabled = true,
            phase = VoiceAnswerPhase.SpeakingNotice,
            lastGrade = grade,
            lastGradedCardId = cardId,
        )
        advanceUntilIdle()
    }

    @Test
    fun `a voice grade drives the same Attempt increment and re-insertion as the equivalent manual rating`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadThreeCards()
            val viewModel = createViewModel()
            advanceUntilIdle()

            emitGrade(grade = VoiceAnswerGrade(sanitizedTranscript = "t", gradePercent = 20, feedback = "missed it"), cardId = "card-1")

            viewModel.state.value.currentCard?.id shouldNotBe "card-1"
            viewModel.state.value.flashcards.map { it.id } shouldContain "card-1"
        }

    @Test
    fun `a grade in the Correct band finishes the card as Mastered, exactly as a manual Correct does`() =
        runTest(mainDispatcherRule.testDispatcher) {
            flashcardRepository.flashcardsBySubcategory[subcategoryId] = Result.success(listOf(flashcard("card-1")))
            stubRoute(route.copy(cardIds = listOf("card-1")))
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.events.test {
                emitGrade(grade = VoiceAnswerGrade(sanitizedTranscript = "t", gradePercent = 95, feedback = "great"), cardId = "card-1")
                awaitItem() shouldBe RatedStudySessionDestination.Back
            }
            viewModel.state.value.masteredCount shouldBe 1
        }

    @Test
    fun `a silence timeout leaves the card's Rating list and best rating unchanged, and re-queues the card`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadTenCards()
            val viewModel = createViewModel().apply { random = Random(FIXED_SEED) }
            advanceUntilIdle()

            emitSilenceTimeout()

            viewModel.state.value.currentCard?.id shouldNotBe "card-1"
            viewModel.state.value.flashcards.map { it.id } shouldContain "card-1"
            while (viewModel.state.value.currentCard?.id != "card-1") {
                viewModel.onRating(FlashcardRating.Correct)
            }

            viewModel.state.value.currentCardRatings shouldBe emptyList()
        }

    @Test
    fun `a silence timeout re-queues within the Failed gap range`() = runTest(mainDispatcherRule.testDispatcher) {
        loadTenCards()
        val viewModel = createViewModel()
        advanceUntilIdle()

        emitSilenceTimeout()

        val index = viewModel.state.value.flashcards.indexOfFirst { it.id == "card-1" }
        (index in StudySessionConfig.FAILED_REQUEUE_MIN_GAP..StudySessionConfig.FAILED_REQUEUE_MAX_GAP) shouldBe true
    }

    @Test
    fun `two silence timeouts do not pause the session, but the third does`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadThreeCards()
            val viewModel = createViewModel()
            advanceUntilIdle()

            repeat(2) { emitSilenceTimeout() }
            viewModel.state.value.isVoiceAnswerPaused shouldBe false

            emitSilenceTimeout()

            viewModel.state.value.isVoiceAnswerPaused shouldBe true
        }

    @Test
    fun `the consecutive silence counter resets on a graded answer, so silence-silence-grade-silence does not pause`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadThreeCards()
            val viewModel = createViewModel()
            advanceUntilIdle()

            repeat(2) { emitSilenceTimeout() }
            emitGrade(
                grade = VoiceAnswerGrade(sanitizedTranscript = "t", gradePercent = 90, feedback = "f"),
                cardId = viewModel.state.value.currentCard?.id.orEmpty(),
            )

            emitSilenceTimeout()

            viewModel.state.value.isVoiceAnswerPaused shouldBe false
        }

    @Test
    fun `pausing after three silences stops playback, exposes the resume affordance, and records no outcome`() =
        runTest(mainDispatcherRule.testDispatcher) {
            loadThreeCards()
            val viewModel = createViewModel()
            advanceUntilIdle()
            voiceGateway.stateFlow.value = VoicePlaybackState(isActive = true, isPlaying = true)
            advanceUntilIdle()
            val masteredBefore = viewModel.state.value.masteredCount
            val flashcardsBefore = viewModel.state.value.flashcards.map { it.id }

            viewModel.events.test {
                repeat(3) { emitSilenceTimeout() }
                expectNoEvents()
            }

            viewModel.state.value.isVoiceAnswerPaused shouldBe true
            voiceGateway.togglePlayPauseCalls shouldBe 1
            voiceGateway.lastVoiceAnswering shouldBe false
            viewModel.state.value.masteredCount shouldBe masteredBefore
            viewModel.state.value.flashcards.map { it.id } shouldBe flashcardsBefore
        }

    @Test
    fun `resuming continues from the same card with the counter reset`() = runTest(mainDispatcherRule.testDispatcher) {
        loadThreeCards()
        val viewModel = createViewModel()
        advanceUntilIdle()
        repeat(3) { emitSilenceTimeout() }
        val cardBeforePause = viewModel.state.value.currentCard?.id

        viewModel.onResumeSession()

        viewModel.state.value.isVoiceAnswerPaused shouldBe false
        viewModel.state.value.currentCard?.id shouldBe cardBeforePause
        voiceGateway.lastVoiceAnswering shouldBe true

        // Counter reset: two more silences must not re-pause.
        repeat(2) { emitSilenceTimeout() }
        viewModel.state.value.isVoiceAnswerPaused shouldBe false
    }

    private companion object {
        const val FIXED_SEED = 42L
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
    var updateQueueCalls = 0
    var lastUpdateQueueCards: List<Flashcard>? = null
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

    override fun updateQueue(cards: List<Flashcard>) {
        updateQueueCalls++
        lastUpdateQueueCards = cards
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
