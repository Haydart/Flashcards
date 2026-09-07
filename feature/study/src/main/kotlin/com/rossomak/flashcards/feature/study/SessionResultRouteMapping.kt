package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.SessionLedgerEntry
import com.rossomak.flashcards.core.domain.model.SessionResult
import java.time.Instant

/**
 * Flattens [this] onto [StudySessionSummaryRoute] for the terminal navigation event — see that
 * type's KDoc for why the ledger's fields carry a `card` prefix and [SessionResult.startedAt]
 * becomes a `Long`.
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
    cardIds = ledger.map { it.cardId },
    cardSubcategoryIds = ledger.map { it.subcategoryId },
    cardStates = ledger.map { it.state },
    cardAttemptsUsed = ledger.map { it.attemptsUsed },
    cardWasPreviouslyMastered = ledger.map { it.wasPreviouslyMastered },
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
    ledger = cardIds.indices.map { index ->
        SessionLedgerEntry(
            cardId = cardIds[index],
            subcategoryId = cardSubcategoryIds[index],
            state = cardStates[index],
            attemptsUsed = cardAttemptsUsed[index],
            wasPreviouslyMastered = cardWasPreviouslyMastered[index],
        )
    },
)
