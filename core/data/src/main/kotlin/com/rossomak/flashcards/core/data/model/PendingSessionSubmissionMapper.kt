package com.rossomak.flashcards.core.data.model

import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.XpConfig
import java.time.Instant

/** `toDto()`/`toDomain()` conversions between [SessionResult] and [PendingSessionSubmissionDto]. */
object PendingSessionSubmissionMapper {

    fun SessionResult.toDto(): PendingSessionSubmissionDto = PendingSessionSubmissionDto(
        id = id,
        mode = mode.name,
        startedAtEpochMillis = startedAt.toEpochMilli(),
        durationSeconds = durationSeconds,
        abandoned = abandoned,
        categoryId = categoryId,
        categoryName = categoryName,
        subcategoryIds = subcategoryIds,
        subcategoryNames = subcategoryNames,
        cardResults = cardResults.map { it.toDto() },
        xpConfig = xpConfig.toDto(),
    )

    fun PendingSessionSubmissionDto.toDomain(): SessionResult = when (StudyMode.valueOf(mode)) {
        StudyMode.Rated -> SessionResult.Rated(
            id = id,
            startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
            durationSeconds = durationSeconds,
            abandoned = abandoned,
            categoryId = categoryId,
            categoryName = categoryName,
            subcategoryIds = subcategoryIds,
            subcategoryNames = subcategoryNames,
            cardResults = cardResults.map { it.toRatedDomain() },
            xpConfig = xpConfig.toDomain(),
        )
        StudyMode.Fast -> SessionResult.Fast(
            id = id,
            startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
            durationSeconds = durationSeconds,
            abandoned = abandoned,
            categoryId = categoryId,
            categoryName = categoryName,
            subcategoryIds = subcategoryIds,
            subcategoryNames = subcategoryNames,
            cardResults = cardResults.map { it.toFastDomain() },
            xpConfig = xpConfig.toDomain(),
        )
    }

    private fun FlashcardResult.toDto(): PendingFlashcardResultDto = PendingFlashcardResultDto(
        cardId = cardId,
        subcategoryId = subcategoryId,
        state = state.name,
        attemptsUsed = (this as? FlashcardResult.Rated)?.attemptsUsed,
        wasPreviouslyMastered = (this as? FlashcardResult.Rated)?.wasPreviouslyMastered,
    )

    private fun PendingFlashcardResultDto.toRatedDomain(): FlashcardResult.Rated = FlashcardResult.Rated(
        cardId = cardId,
        subcategoryId = subcategoryId,
        state = FlashcardStudyProgressState.valueOf(state),
        attemptsUsed = requireNotNull(attemptsUsed) { "Rated pending card result '$cardId' missing attemptsUsed" },
        wasPreviouslyMastered = requireNotNull(wasPreviouslyMastered) {
            "Rated pending card result '$cardId' missing wasPreviouslyMastered"
        },
    )

    private fun PendingFlashcardResultDto.toFastDomain(): FlashcardResult.Fast = FlashcardResult.Fast(
        cardId = cardId,
        subcategoryId = subcategoryId,
        state = FlashcardStudyProgressState.valueOf(state),
    )

    private fun XpConfig.toDto(): PendingXpConfigDto = PendingXpConfigDto(
        newCardStudied = newCardStudied,
        cardMastered = cardMastered,
        cardPartial = cardPartial,
        masteryDefended = masteryDefended,
        cardDemastered = cardDemastered,
        sessionCompleted = sessionCompleted,
        dailyGoalMet = dailyGoalMet,
        streakPerDay = streakPerDay,
        streakMaxPerDay = streakMaxPerDay,
        minuteStudied = minuteStudied,
        levelCurveBase = levelCurveBase,
        levelCurveExponent = levelCurveExponent,
    )

    private fun PendingXpConfigDto.toDomain(): XpConfig = XpConfig(
        newCardStudied = newCardStudied,
        cardMastered = cardMastered,
        cardPartial = cardPartial,
        masteryDefended = masteryDefended,
        cardDemastered = cardDemastered,
        sessionCompleted = sessionCompleted,
        dailyGoalMet = dailyGoalMet,
        streakPerDay = streakPerDay,
        streakMaxPerDay = streakMaxPerDay,
        minuteStudied = minuteStudied,
        levelCurveBase = levelCurveBase,
        levelCurveExponent = levelCurveExponent,
    )
}
