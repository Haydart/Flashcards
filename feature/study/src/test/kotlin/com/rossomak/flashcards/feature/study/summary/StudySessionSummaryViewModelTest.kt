package com.rossomak.flashcards.feature.study.summary

import androidx.lifecycle.SavedStateHandle
import com.rossomak.flashcards.core.domain.model.FlashcardProgressState
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.navigation.RouteDecoder
import com.rossomak.flashcards.feature.study.StudySessionSummaryRoute
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * There is no past-session fallback path to test here — this route only ever carries a fresh
 * result (spec 03 ticket 02), so every case below is the same single load path.
 */
class StudySessionSummaryViewModelTest {

    private val savedStateHandle: SavedStateHandle = mockk()

    @Before
    fun setUp() {
        mockkObject(RouteDecoder)
    }

    @After
    fun tearDown() {
        unmockkObject(RouteDecoder)
    }

    private fun stubRoute(route: StudySessionSummaryRoute) {
        every { RouteDecoder.decode(any<() -> StudySessionSummaryRoute>()) } returns route
    }

    private fun ratedRoute(
        abandoned: Boolean = false,
        cardStates: List<FlashcardProgressState> = listOf(
            FlashcardProgressState.Mastered,
            FlashcardProgressState.Mastered,
            FlashcardProgressState.Partial,
            FlashcardProgressState.Failed,
        ),
    ): StudySessionSummaryRoute {
        val cardIds = cardStates.indices.map { "card-$it" }
        return StudySessionSummaryRoute(
            sessionId = "session-1",
            mode = StudyMode.Rated,
            startedAtEpochSecond = 0L,
            durationSeconds = 120,
            abandoned = abandoned,
            categoryId = "cat-1",
            categoryName = "Category",
            subcategoryIds = listOf("sub-1"),
            subcategoryNames = listOf("Subcategory"),
            cardIds = cardIds,
            cardSubcategoryIds = cardIds.map { "sub-1" },
            cardStates = cardStates,
            cardAttemptsUsed = cardStates.map { 1 },
            cardWasPreviouslyMastered = cardStates.map { false },
        )
    }

    @Test
    fun `a Rated result exposes mode, duration, studied count and the three Terminal State counts`() {
        stubRoute(ratedRoute())

        val viewModel = StudySessionSummaryViewModel(savedStateHandle)

        with(viewModel.state.value) {
            mode shouldBe StudyMode.Rated
            durationSeconds shouldBe 120
            studiedCount shouldBe 4
            abandoned shouldBe false
            masteredCount shouldBe 2
            partialCount shouldBe 1
            failedCount shouldBe 1
        }
    }

    @Test
    fun `an abandoned Rated result exposes the abandoned flag set`() {
        stubRoute(ratedRoute(abandoned = true))

        val viewModel = StudySessionSummaryViewModel(savedStateHandle)

        viewModel.state.value.abandoned shouldBe true
    }

    @Test
    fun `a Fast result exposes the reduced set with zero for every Terminal State count`() {
        val cardIds = listOf("card-1", "card-2")
        stubRoute(
            StudySessionSummaryRoute(
                sessionId = "session-2",
                mode = StudyMode.Fast,
                startedAtEpochSecond = 0L,
                durationSeconds = 60,
                abandoned = false,
                categoryId = "cat-1",
                categoryName = "Category",
                subcategoryIds = listOf("sub-1"),
                subcategoryNames = listOf("Subcategory"),
                cardIds = cardIds,
                cardSubcategoryIds = cardIds.map { "sub-1" },
                cardStates = cardIds.map { FlashcardProgressState.Seen },
                cardAttemptsUsed = cardIds.map { 0 },
                cardWasPreviouslyMastered = cardIds.map { false },
            ),
        )

        val viewModel = StudySessionSummaryViewModel(savedStateHandle)

        with(viewModel.state.value) {
            mode shouldBe StudyMode.Fast
            studiedCount shouldBe 2
            masteredCount shouldBe 0
            partialCount shouldBe 0
            failedCount shouldBe 0
        }
    }
}
