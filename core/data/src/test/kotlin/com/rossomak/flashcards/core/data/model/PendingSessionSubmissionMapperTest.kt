package com.rossomak.flashcards.core.data.model

import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionMapper.toDomain
import com.rossomak.flashcards.core.data.model.PendingSessionSubmissionMapper.toDto
import com.rossomak.flashcards.core.domain.model.DailyGoal
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.XpConfig
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.ZoneId
import org.junit.Test

class PendingSessionSubmissionMapperTest {

    private fun ratedSessionResult(): SessionResult.Rated = SessionResult.Rated(
        id = "session-1",
        startedAt = Instant.parse("2026-09-08T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        cardResults = listOf(
            FlashcardResult.Rated(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Mastered,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
        ),
        studyDate = "2026-09-08",
        studyDateUtcOffsetMinutes = -300,
        dailyGoalMinutes = 20,
        xpConfig = XpConfig(newCardStudied = 42),
    )

    private fun fastSessionResult(): SessionResult.Fast = SessionResult.Fast(
        id = "session-2",
        startedAt = Instant.parse("2026-09-08T11:00:00Z"),
        durationSeconds = 30,
        abandoned = true,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        cardResults = listOf(FlashcardResult.Fast(cardId = "card-1", subcategoryId = "sub-1", state = FlashcardStudyProgressState.Seen)),
        studyDate = "2026-09-08",
        studyDateUtcOffsetMinutes = -300,
        dailyGoalMinutes = 20,
    )

    @Test
    fun `toDto then toDomain round-trips a Rated session losslessly, including xpConfig`() {
        val original = ratedSessionResult()

        val roundTripped = original.toDto().toDomain()

        roundTripped shouldBe original
    }

    @Test
    fun `toDto then toDomain round-trips a Fast session losslessly`() {
        val original = fastSessionResult()

        val roundTripped = original.toDto().toDomain()

        roundTripped shouldBe original
    }

    @Test
    fun `toDto omits attemptsUsed and wasPreviouslyMastered for a Fast card result`() {
        val dto = fastSessionResult().toDto()

        val cardResult = dto.cardResults.single()
        cardResult.attemptsUsed shouldBe null
        cardResult.wasPreviouslyMastered shouldBe null
    }

    @Test
    fun `toDomain derives studyDate from startedAtEpochMillis for a legacy entry with a blank studyDate`() {
        val legacyDto = fastSessionResult().toDto().copy(studyDate = "")

        val migrated = legacyDto.toDomain()

        migrated.studyDate shouldBe Instant.parse("2026-09-08T11:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }

    @Test
    fun `toDomain substitutes the default daily goal for a legacy entry with a non-positive dailyGoalMinutes`() {
        val legacyDto = fastSessionResult().toDto().copy(dailyGoalMinutes = 0)

        val migrated = legacyDto.toDomain()

        migrated.dailyGoalMinutes shouldBe DailyGoal.DEFAULT_MINUTES
    }

    @Test
    fun `toDomain leaves a well-formed entry's studyDate and dailyGoalMinutes untouched`() {
        val dto = fastSessionResult().toDto()

        val migrated = dto.toDomain()

        migrated.studyDate shouldBe dto.studyDate
        migrated.dailyGoalMinutes shouldBe dto.dailyGoalMinutes
    }
}
