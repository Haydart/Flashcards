package com.rossomak.flashcards.core.data.model

import kotlinx.serialization.Serializable

/**
 * Full-fidelity, lossless mirror of a domain
 * [com.rossomak.flashcards.core.domain.model.SessionResult] for the local durable delivery queue
 * (ticket 03) — carries every field the domain type carries, both `Rated`/`Fast` variants, including
 * [xpConfig]. **Independent of** [com.rossomak.flashcards.core.data.repository.RemoteSessionSubmissionRepository]'s
 * own wire payload: that one is a network-wire subset (no `xpConfig`); this DTO's job is a lossless
 * round trip through an app restart, not matching what the network call sends.
 *
 * [mode] and [startedAtEpochMillis] use the same string/epoch-millis encoding as the network payload
 * (`StudyMode.name`, `Instant.toEpochMilli()`) rather than relying on kotlinx.serialization's default
 * enum/`Instant` handling, for the same reason: no contextual serializer to wire up, and one
 * unsurprising encoding this whole data layer already uses elsewhere.
 *
 * See [PendingSessionSubmissionMapper] for the `toDto()`/`toDomain()` conversions, and
 * [com.rossomak.flashcards.core.data.source.FilePendingSessionSubmissionLocalDataSource] for where a
 * JSON array of these gets persisted.
 *
 * **A field added to [com.rossomak.flashcards.core.domain.model.SessionResult] needs updating in two
 * independent places, not just one**: here (plus [PendingSessionSubmissionMapper]) for the durable
 * queue, and separately in [com.rossomak.flashcards.core.data.repository.RemoteSessionSubmissionRepository]'s
 * own `toPayload()` for the network wire shape — the two are deliberately different subsets (this one
 * is full-fidelity, that one omits `xpConfig`), so neither can be derived from the other, and nothing
 * enforces they stay in sync beyond this note.
 */
@Serializable
data class PendingSessionSubmissionDto(
    val id: String,
    val mode: String,
    val startedAtEpochMillis: Long,
    val durationSeconds: Int,
    val abandoned: Boolean,
    val categoryId: String,
    val categoryName: String,
    val subcategoryIds: List<String>,
    val subcategoryNames: List<String>,
    val cardResults: List<PendingFlashcardResultDto>,
    val studyDate: String,
    val dailyGoalMinutes: Int,
    val xpConfig: PendingXpConfigDto,
)

/**
 * Mirrors [com.rossomak.flashcards.core.domain.model.FlashcardResult]'s two variants in one shape:
 * [attemptsUsed] and [wasPreviouslyMastered] are `null` for a `Fast` entry, present for a `Rated`
 * one — genuinely absent, not zeroed/falsed, matching the domain type's own sealed split.
 */
@Serializable
data class PendingFlashcardResultDto(
    val cardId: String,
    val subcategoryId: String,
    val state: String,
    val attemptsUsed: Int? = null,
    val wasPreviouslyMastered: Boolean? = null,
)

/** Field-for-field mirror of [com.rossomak.flashcards.core.domain.model.XpConfig]. */
@Serializable
data class PendingXpConfigDto(
    val newCardStudied: Int,
    val cardMastered: Int,
    val cardPartial: Int,
    val masteryDefended: Int,
    val cardDemastered: Int,
    val sessionCompleted: Int,
    val dailyGoalMet: Int,
    val streakPerDay: Int,
    val streakMaxPerDay: Int,
    val minuteStudied: Int,
    val levelCurveBase: Double,
    val levelCurveExponent: Double,
)
