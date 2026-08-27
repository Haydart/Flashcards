package com.rossomak.flashcards.feature.study.preview

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.StudySessionPreferences
import com.rossomak.flashcards.core.domain.model.VoiceSettings as SavedVoiceSettings
import com.rossomak.flashcards.core.domain.repository.FakeFlashcardRepository
import com.rossomak.flashcards.core.domain.repository.FakeStudySessionPreferencesRepository
import com.rossomak.flashcards.core.domain.usecase.ObserveStudySessionPreferencesUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveStudySessionPreferenceUseCase
import com.rossomak.flashcards.core.domain.usecase.SelectSessionFlashcardsUseCase
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.feature.study.PreviewStudySessionRoute
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Attempts
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Filters
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Length
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.ReadAloud
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Sort
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceAnswering
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceSettings
import com.rossomak.flashcards.testutil.MainDispatcherRule
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewStudySessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle: SavedStateHandle = mockk()
    private val flashcardRepository = FakeFlashcardRepository()
    private val studySessionPreferencesRepository = FakeStudySessionPreferencesRepository()
    private val voiceSettingsController: VoiceSettingsController = mockk(relaxed = true)

    /** Anything but [StudySessionConfig.DEFAULT_RATED_ATTEMPTS], so a commit is visible. */
    private val strictAttempts = StudySessionConfig.MIN_RATED_ATTEMPTS

    /** Anything but [StudySessionConfig.DEFAULT_LENGTH], so a seeded value is visible. */
    private val seededLength = StudySessionConfig.MIN_LENGTH

    private val categoryId = "android"
    private val categoryName = "Android"
    private val subcategoryId = "android-compose"
    private val subcategoryName = "Compose"

    private val singleTopicRoute = PreviewStudySessionRoute(
        categoryId = categoryId,
        categoryName = categoryName,
        subcategoryIds = listOf(subcategoryId),
        subcategoryNames = listOf(subcategoryName),
    )

    private val multiTopicRoute = singleTopicRoute.copy(
        subcategoryIds = listOf("android-compose", "android-coroutines"),
        subcategoryNames = listOf("Compose", "Coroutines"),
    )

    @Before
    fun setUp() {
        mockkObject(RouteDecoder)
    }

    @After
    fun tearDown() {
        unmockkObject(RouteDecoder)
    }

    private fun stubRoute(route: PreviewStudySessionRoute) {
        every { RouteDecoder.decode(any<() -> PreviewStudySessionRoute>()) } returns route
    }

    private fun createViewModel(): PreviewStudySessionViewModel = PreviewStudySessionViewModel(
        savedStateHandle,
        SelectSessionFlashcardsUseCase(flashcardRepository),
        ObserveStudySessionPreferencesUseCase(studySessionPreferencesRepository),
        SaveStudySessionPreferenceUseCase(studySessionPreferencesRepository),
        voiceSettingsController,
    )

    private fun flashcard(
        id: String,
        subcategoryId: String = this.subcategoryId,
        tags: List<String> = listOf("General"),
        difficulty: Int = 5
    ): Flashcard = Flashcard(
        id = id,
        subcategoryId = subcategoryId,
        tags = tags,
        question = "question-$id",
        answer = "answer-$id",
        difficulty = difficulty,
        questionCode = null,
        answerCode = null,
        questionSpoken = null,
        answerSpoken = null,
        extendedContext = null
    )

    @Test
    fun `selection caps card count at session size`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.selectedCardCount shouldBe 20
        viewModel.state.value.isLoading shouldBe false
    }

    @Test
    fun `selection uses whole pool when smaller than session size`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..5).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.selectedCardCount shouldBe 5
    }

    @Test
    fun `routed tag filter seeds the config and keeps only cards carrying an active tag`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(singleTopicRoute.copy(filterTagIds = listOf("State")))
            flashcardRepository.flashcardsToReturn = Result.success(
                listOf(
                    flashcard(id = "card-1", tags = listOf("State")),
                    flashcard(id = "card-2", tags = listOf("Modifiers")),
                    flashcard(id = "card-3", tags = listOf("State", "Modifiers")),
                )
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.state.value.config.tagIds shouldBe setOf("State")
            viewModel.state.value.selectedCardCount shouldBe 2
        }

    @Test
    fun `multi subcategory route pools cards across subcategories`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(multiTopicRoute)
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success(listOf(flashcard(id = "card-1")))
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success(listOf(flashcard(id = "card-2", subcategoryId = "android-coroutines")))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.selectedCardCount shouldBe 2
    }

    @Test
    fun `failed fetch surfaces error`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.error shouldBe "Could not load flashcards"
        viewModel.state.value.canStart shouldBe false
    }

    @Test
    fun `available tags come from the pool for single topic sessions only`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", tags = listOf("State")),
                flashcard(id = "card-2", tags = listOf("Modifiers", "State")),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.availableTags shouldBe listOf("Modifiers", "State")

        stubRoute(multiTopicRoute)
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success(listOf(flashcard(id = "card-1", tags = listOf("State"))))
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success(listOf(flashcard(id = "card-2", subcategoryId = "android-coroutines")))

        val multiTopicViewModel = createViewModel()
        advanceUntilIdle()

        multiTopicViewModel.state.value.availableTags shouldBe emptyList()
    }

    @Test
    fun `a draft change leaves the committed config untouched until confirm`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Mode(draft = viewModel.state.value.config.mode)))
        viewModel.onDialogEvent(
            DraftChange(Mode(draft = StudyMode.Fast))
        )

        viewModel.state.value.activeDialog shouldBe Mode(draft = StudyMode.Fast)
        viewModel.state.value.config.mode shouldBe StudyMode.Rated
    }

    @Test
    fun `dismissing discards the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Sort(draft = viewModel.state.value.config.sortOrder)))
        viewModel.onDialogEvent(
            DraftChange(Sort(draft = FlashcardSortOrder.HardestFirst))
        )
        viewModel.onDialogEvent(Dismiss)

        viewModel.state.value.activeDialog shouldBe null
        viewModel.state.value.config.sortOrder shouldBe FlashcardSortOrder.Default
    }

    @Test
    fun `confirming the mode dialog commits the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Mode(draft = viewModel.state.value.config.mode)))
        viewModel.onDialogEvent(
            DraftChange(Mode(draft = StudyMode.Fast))
        )
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.mode shouldBe StudyMode.Fast
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `confirming the length dialog reselects at the new count`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..30).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Length(draft = viewModel.state.value.config.length)))
        viewModel.onDialogEvent(DraftChange(Length(draft = 10)))
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.length shouldBe 10
        viewModel.state.value.selectedCardCount shouldBe 10
    }

    @Test
    fun `confirming the attempts dialog commits the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Attempts(draft = viewModel.state.value.config.ratedAttempts)))
        viewModel.onDialogEvent(DraftChange(Attempts(draft = strictAttempts)))
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.ratedAttempts shouldBe strictAttempts
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `dismissing the attempts dialog discards the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        val committedAttempts = viewModel.state.value.config.ratedAttempts
        viewModel.onDialogEvent(Open(Attempts(draft = committedAttempts)))
        viewModel.onDialogEvent(DraftChange(Attempts(draft = strictAttempts)))
        viewModel.onDialogEvent(Dismiss)
        advanceUntilIdle()

        viewModel.state.value.config.ratedAttempts shouldBe committedAttempts
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `confirming the read-aloud dialog commits the draft`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(ReadAloud(draft = viewModel.state.value.config.readAloudEnabled)))
        viewModel.onDialogEvent(DraftChange(ReadAloud(draft = true)))
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.readAloudEnabled shouldBe true
        viewModel.state.value.activeDialog shouldBe null
    }

    @Test
    fun `confirming the filters dialog narrows the pool by difficulty and tags`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", tags = listOf("State"), difficulty = 2),
                flashcard(id = "card-2", tags = listOf("State"), difficulty = 5),
                flashcard(id = "card-3", tags = listOf("Modifiers"), difficulty = 5),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(
            Filters(
                draft = FlashcardFilters(
                    selectedTags = viewModel.state.value.config.tagIds,
                    difficultyRange = viewModel.state.value.config.difficultyRange,
                ),
                availableTags = viewModel.state.value.availableTags,
            )
        ))
        val filtersDialog = viewModel.state.value.activeDialog as Filters
        viewModel.onDialogEvent(
            DraftChange(
                filtersDialog.copy(draft = FlashcardFilters(selectedTags = setOf("State"), difficultyRange = 4..6))
            )
        )
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        viewModel.state.value.config.tagIds shouldBe setOf("State")
        viewModel.state.value.config.difficultyRange shouldBe 4..6
        viewModel.state.value.selectedCardCount shouldBe 1
    }

    @Test
    fun `confirming the sort dialog orders session cards easiest first`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", difficulty = 8),
                flashcard(id = "card-2", difficulty = 2),
                flashcard(id = "card-3", difficulty = 5),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Sort(draft = viewModel.state.value.config.sortOrder)))
        viewModel.onDialogEvent(
            DraftChange(Sort(draft = FlashcardSortOrder.EasiestFirst))
        )
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.state.value.config.sortOrder shouldBe FlashcardSortOrder.EasiestFirst
        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.cardIds shouldBe listOf("card-2", "card-3", "card-1")
        }
    }

    @Test
    fun `confirming the sort dialog orders session cards hardest first`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(
                flashcard(id = "card-1", difficulty = 8),
                flashcard(id = "card-2", difficulty = 2),
                flashcard(id = "card-3", difficulty = 5),
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Sort(draft = viewModel.state.value.config.sortOrder)))
        viewModel.onDialogEvent(
            DraftChange(Sort(draft = FlashcardSortOrder.HardestFirst))
        )
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.cardIds shouldBe listOf("card-1", "card-3", "card-2")
        }
    }

    @Test
    fun `seeded study session preferences reach the config before the first card selection`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(singleTopicRoute)
            studySessionPreferencesRepository.preferences.value = StudySessionPreferences(
                defaultStudyMode = StudyMode.Fast,
                sessionLength = seededLength,
                sortOrder = FlashcardSortOrder.HardestFirst,
            )
            flashcardRepository.flashcardsToReturn =
                Result.success((1..30).map { index -> flashcard(id = "card-$index") })

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.state.value.config.mode shouldBe StudyMode.Fast
            viewModel.state.value.config.length shouldBe seededLength
            viewModel.state.value.config.sortOrder shouldBe FlashcardSortOrder.HardestFirst
            viewModel.state.value.selectedCardCount shouldBe seededLength
        }

    @Test
    fun `confirming with keepAsDefault true writes the preference`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Length(draft = viewModel.state.value.config.length)))
        viewModel.onDialogEvent(DraftChange(Length(draft = 10, keepAsDefault = true)))
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        studySessionPreferencesRepository.preferences.value.sessionLength shouldBe 10
        viewModel.state.value.config.length shouldBe 10
    }

    @Test
    fun `confirming with keepAsDefault false applies the draft but writes nothing`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(singleTopicRoute)
            flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

            val viewModel = createViewModel()
            advanceUntilIdle()
            val committedLength = studySessionPreferencesRepository.preferences.value.sessionLength
            viewModel.onDialogEvent(Open(Length(draft = viewModel.state.value.config.length)))
            viewModel.onDialogEvent(DraftChange(Length(draft = 10, keepAsDefault = false)))
            viewModel.onDialogEvent(Confirm)
            advanceUntilIdle()

            studySessionPreferencesRepository.preferences.value.sessionLength shouldBe committedLength
            viewModel.state.value.config.length shouldBe 10
        }

    @Test
    fun `confirming filters never writes a default`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(
            listOf(flashcard(id = "card-1", tags = listOf("State"))),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        val defaultsBeforeConfirm = studySessionPreferencesRepository.preferences.value
        viewModel.onDialogEvent(
            Open(
                Filters(
                    draft = FlashcardFilters(
                        selectedTags = viewModel.state.value.config.tagIds,
                        difficultyRange = viewModel.state.value.config.difficultyRange,
                    ),
                    availableTags = viewModel.state.value.availableTags,
                ),
            ),
        )
        val filtersDialog = viewModel.state.value.activeDialog as Filters
        viewModel.onDialogEvent(
            DraftChange(
                filtersDialog.copy(
                    draft = FlashcardFilters(
                        selectedTags = setOf("State"),
                        difficultyRange = viewModel.state.value.config.difficultyRange,
                    ),
                ),
            ),
        )
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        studySessionPreferencesRepository.preferences.value shouldBe defaultsBeforeConfirm
    }

    @Test
    fun `confirming the voice dialog with keepAsDefault true writes the preference`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(singleTopicRoute)
            flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))
            val voiceSettings = SavedVoiceSettings(speechRate = 1.5f, voiceId = "voice-1")
            every { voiceSettingsController.seedDraft(any()) } returns VoiceSettingsDraftState()

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onDialogEvent(Open(VoiceSettings()))
            val draft = (viewModel.state.value.activeDialog as VoiceSettings).draft
                .copy(draftSpeed = voiceSettings.speechRate, draftVoiceId = voiceSettings.voiceId)
            viewModel.onDialogEvent(DraftChange(VoiceSettings(draft = draft, keepAsDefault = true)))
            viewModel.onDialogEvent(Confirm)
            advanceUntilIdle()

            studySessionPreferencesRepository.preferences.value.voiceSettings shouldBe voiceSettings
            viewModel.state.value.config.voiceSettings shouldBe voiceSettings
        }

    @Test
    fun `confirming the voice dialog with keepAsDefault false applies the draft but writes nothing`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(singleTopicRoute)
            flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))
            val voiceSettings = SavedVoiceSettings(speechRate = 1.5f, voiceId = "voice-1")
            every { voiceSettingsController.seedDraft(any()) } returns VoiceSettingsDraftState()

            val viewModel = createViewModel()
            advanceUntilIdle()
            val committedVoiceSettings = studySessionPreferencesRepository.preferences.value.voiceSettings
            viewModel.onDialogEvent(Open(VoiceSettings()))
            val draft = (viewModel.state.value.activeDialog as VoiceSettings).draft
                .copy(draftSpeed = voiceSettings.speechRate, draftVoiceId = voiceSettings.voiceId)
            viewModel.onDialogEvent(DraftChange(VoiceSettings(draft = draft, keepAsDefault = false)))
            viewModel.onDialogEvent(Confirm)
            advanceUntilIdle()

            studySessionPreferencesRepository.preferences.value.voiceSettings shouldBe committedVoiceSettings
            viewModel.state.value.config.voiceSettings shouldBe voiceSettings
        }

    @Test
    fun `onStartSession emits StudySession route with selected cards, mode, voice answering, attempts and read-aloud`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubRoute(singleTopicRoute)
            flashcardRepository.flashcardsToReturn = Result.success(
                listOf(flashcard(id = "card-1"), flashcard(id = "card-2"))
            )

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onDialogEvent(Open(VoiceAnswering(draft = viewModel.state.value.config.voiceAnsweringEnabled)))
            viewModel.onDialogEvent(
                DraftChange(VoiceAnswering(draft = true))
            )
            viewModel.onDialogEvent(Confirm)
            viewModel.onDialogEvent(Open(Attempts(draft = viewModel.state.value.config.ratedAttempts)))
            viewModel.onDialogEvent(DraftChange(Attempts(draft = 5)))
            viewModel.onDialogEvent(Confirm)
            viewModel.onDialogEvent(Open(ReadAloud(draft = viewModel.state.value.config.readAloudEnabled)))
            viewModel.onDialogEvent(DraftChange(ReadAloud(draft = true)))
            viewModel.onDialogEvent(Confirm)
            advanceUntilIdle()
            viewModel.onStartSession()

            viewModel.events.test {
                val destination = awaitItem() as PreviewStudySessionDestination.StudySession
                destination.route.categoryId shouldBe categoryId
                destination.route.sessionTitle shouldBe subcategoryName
                destination.route.subcategoryIds shouldBe listOf(subcategoryId)
                destination.route.cardIds shouldContainAll listOf("card-1", "card-2")
                destination.route.studyMode shouldBe StudyMode.Rated
                destination.route.voiceAnsweringEnabled shouldBe true
                destination.route.ratedAttempts shouldBe 5
                destination.route.readAloudEnabled shouldBe true
            }
        }

    @Test
    fun `onStartSession carries the confirmed voice settings on the route`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))
        val voiceSettings = SavedVoiceSettings(speechRate = 1.5f, voiceId = "voice-1")
        every { voiceSettingsController.seedDraft(any()) } returns VoiceSettingsDraftState()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(VoiceSettings()))
        val draft = (viewModel.state.value.activeDialog as VoiceSettings).draft
            .copy(draftSpeed = voiceSettings.speechRate, draftVoiceId = voiceSettings.voiceId)
        viewModel.onDialogEvent(DraftChange(VoiceSettings(draft = draft)))
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.voiceSettings shouldBe voiceSettings
        }
    }

    @Test
    fun `confirming the voice settings dialog stops preview playback`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        every { voiceSettingsController.seedDraft(any()) } returns VoiceSettingsDraftState()

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(VoiceSettings()))
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        verify { voiceSettingsController.stopPreview() }
    }

    @Test
    fun `confirming a non-voice dialog never touches preview playback`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onDialogEvent(Open(Mode(draft = StudyMode.Fast)))
        viewModel.onDialogEvent(Confirm)
        advanceUntilIdle()

        verify(exactly = 0) { voiceSettingsController.stopPreview() }
    }

    @Test
    fun `onStartSession with empty pool emits nothing`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun `onStartSession ignores re-entrant calls while a session is already pending`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStartSession()
        viewModel.onStartSession()

        viewModel.events.test {
            awaitItem() as PreviewStudySessionDestination.StudySession
            expectNoEvents()
        }
    }

    @Test
    fun `onRetry recovers from a previous failure and loads the pool`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn = Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.state.value.error shouldBe "Could not load flashcards"

        flashcardRepository.flashcardsToReturn = Result.success(listOf(flashcard(id = "card-1")))
        viewModel.onRetry()
        advanceUntilIdle()

        viewModel.state.value.error shouldBe null
        viewModel.state.value.selectedCardCount shouldBe 1
    }

    @Test
    fun `sessionTitle uses category name for multi topic sessions`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(multiTopicRoute)
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success(listOf(flashcard(id = "card-1")))
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success(listOf(flashcard(id = "card-2", subcategoryId = "android-coroutines")))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStartSession()

        viewModel.events.test {
            val destination = awaitItem() as PreviewStudySessionDestination.StudySession
            destination.route.sessionTitle shouldBe categoryName
        }
    }

    @Test
    fun `estimatedMinutes rounds up to the nearest minute`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        flashcardRepository.flashcardsToReturn =
            Result.success((1..5).map { index -> flashcard(id = "card-$index") })

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 5 cards * 40s/card = 200s -> ceil(200/60) = 4 minutes
        viewModel.state.value.estimatedMinutes shouldBe 4
    }

    @Test
    fun `quick session on a single subcategory can still rerandomize`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute.copy(isQuickSession = true))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.state.value.canRerandomize.shouldBeTrue()
    }

    @Test
    fun `onRerandomize redraws with a new seed, keeping session size`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(multiTopicRoute)
        flashcardRepository.flashcardsBySubcategory["android-compose"] =
            Result.success((1..30).map { index -> flashcard(id = "compose-$index") })
        flashcardRepository.flashcardsBySubcategory["android-coroutines"] =
            Result.success((1..30).map { index -> flashcard(id = "coroutines-$index", subcategoryId = "android-coroutines") })

        val viewModel = createViewModel()
        advanceUntilIdle()
        val cardIdsBeforeRerandomize = viewModel.selectedCardIds
        val seedBeforeRerandomize = viewModel.state.value.config.seed

        viewModel.onRerandomize()
        advanceUntilIdle()

        viewModel.state.value.config.seed shouldNotBe seedBeforeRerandomize
        viewModel.state.value.selectedCardCount shouldBe 20
        viewModel.selectedCardIds shouldNotBe cardIdsBeforeRerandomize
    }

    @Test
    fun `single topic route cannot rerandomize, multi topic can`() = runTest(mainDispatcherRule.testDispatcher) {
        stubRoute(singleTopicRoute)
        createViewModel().state.value.canRerandomize shouldBe false

        stubRoute(multiTopicRoute)
        createViewModel().state.value.canRerandomize.shouldBeTrue()
    }
}
