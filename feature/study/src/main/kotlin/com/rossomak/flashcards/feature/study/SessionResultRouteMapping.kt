package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import java.time.Instant

/**
 * Flattens [this] onto [StudySessionSummaryRoute] for the terminal navigation event — see that
 * type's KDoc for why the [SessionResult.cardResults] fields carry a `card` prefix, why
 * [cardAttemptsUsed]/[cardWasPreviouslyMastered] are `null` for a Fast result, and why
 * [SessionResult.startedAt] becomes a `Long`.
 */
fun SessionResult.toSummaryRoute(): StudySessionSummaryRoute = StudySessionSummaryRoute(
    sessionId = id,
    mode = mode,
    startedAtEpochSecond = startedAt.epochSecond,
    durationSeconds = durationSeconds,
    abandoned = abandoned,
    categoryId = categoryId,
    categoryName = categoryName,
    subcategoryIds = subcategoryIds,
    subcategoryNames = subcategoryNames,
    cardIds = cardResults.map { it.cardId },
    cardSubcategoryIds = cardResults.map { it.subcategoryId },
    cardStates = cardResults.map { it.state },
    cardAttemptsUsed = if (mode == StudyMode.Rated) cardResults.map { it.attemptsUsed } else null,
    cardWasPreviouslyMastered = if (mode == StudyMode.Rated) cardResults.map { it.wasPreviouslyMastered } else null,
)

/** The inverse of [toSummaryRoute] — how the Summary ViewModel reads the route back into a [SessionResult]. */
fun StudySessionSummaryRoute.toSessionResult(): SessionResult = SessionResult(
    id = sessionId,
    mode = mode,
    startedAt = Instant.ofEpochSecond(startedAtEpochSecond),
    durationSeconds = durationSeconds,
    abandoned = abandoned,
    categoryId = categoryId,
    categoryName = categoryName,
    subcategoryIds = subcategoryIds,
    subcategoryNames = subcategoryNames,
    cardResults = cardIds.indices.map { index ->
        FlashcardResult(
            cardId = cardIds[index],
            subcategoryId = cardSubcategoryIds[index],
            state = cardStates[index],
            // Fast never carries these (Rated-only, ADR-0014) — 0/false are Fast's real values,
            // not placeholders, matching what FastStudySessionViewModel.sealFastCardResults seals.
            attemptsUsed = cardAttemptsUsed?.get(index) ?: 0,
            wasPreviouslyMastered = cardWasPreviouslyMastered?.get(index) ?: false,
        )
    },
)
