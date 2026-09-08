package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import java.time.Instant

/**
 * Flattens [this] onto [StudySessionSummaryRoute] for the terminal navigation event — see that
 * type's KDoc for why the [SessionResult.cardResults] fields carry a `card` prefix, why
 * [cardAttemptsUsed]/[cardWasPreviouslyMastered] are `null` for a [SessionResult.Fast] result, and
 * why [SessionResult.startedAt] becomes a `Long`.
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
    cardAttemptsUsed = when (this) {
        is SessionResult.Rated -> cardResults.map { it.attemptsUsed }
        is SessionResult.Fast -> null
    },
    cardWasPreviouslyMastered = when (this) {
        is SessionResult.Rated -> cardResults.map { it.wasPreviouslyMastered }
        is SessionResult.Fast -> null
    },
    xpNewCardStudied = xpConfig.newCardStudied,
    xpCardMastered = xpConfig.cardMastered,
    xpCardPartial = xpConfig.cardPartial,
    xpMasteryDefended = xpConfig.masteryDefended,
    xpCardDemastered = xpConfig.cardDemastered,
    xpSessionCompleted = xpConfig.sessionCompleted,
    xpDailyGoalMet = xpConfig.dailyGoalMet,
    xpStreakPerDay = xpConfig.streakPerDay,
    xpStreakMaxPerDay = xpConfig.streakMaxPerDay,
    xpMinuteStudied = xpConfig.minuteStudied,
    xpLevelCurveBase = xpConfig.levelCurveBase,
    xpLevelCurveExponent = xpConfig.levelCurveExponent,
)

/** The inverse of [toSummaryRoute] — how the Summary ViewModel reads the route back into a [SessionResult]. */
fun StudySessionSummaryRoute.toSessionResult(): SessionResult = when (mode) {
    StudyMode.Rated -> SessionResult.Rated(
        id = sessionId,
        startedAt = Instant.ofEpochSecond(startedAtEpochSecond),
        durationSeconds = durationSeconds,
        abandoned = abandoned,
        categoryId = categoryId,
        categoryName = categoryName,
        subcategoryIds = subcategoryIds,
        subcategoryNames = subcategoryNames,
        cardResults = cardIds.indices.map { index ->
            FlashcardResult.Rated(
                cardId = cardIds[index],
                subcategoryId = cardSubcategoryIds[index],
                state = cardStates[index],
                attemptsUsed = requireNotNull(cardAttemptsUsed) { "a Rated route always carries cardAttemptsUsed" }[index],
                wasPreviouslyMastered = requireNotNull(cardWasPreviouslyMastered) {
                    "a Rated route always carries cardWasPreviouslyMastered"
                }[index],
            )
        },
        xpConfig = xpConfig,
    )
    StudyMode.Fast -> SessionResult.Fast(
        id = sessionId,
        startedAt = Instant.ofEpochSecond(startedAtEpochSecond),
        durationSeconds = durationSeconds,
        abandoned = abandoned,
        categoryId = categoryId,
        categoryName = categoryName,
        subcategoryIds = subcategoryIds,
        subcategoryNames = subcategoryNames,
        cardResults = cardIds.indices.map { index ->
            FlashcardResult.Fast(
                cardId = cardIds[index],
                subcategoryId = cardSubcategoryIds[index],
                state = cardStates[index],
            )
        },
        xpConfig = xpConfig,
    )
}
