package com.rossomak.flashcards.core.domain.model

import java.time.Instant

/**
 * One card's outcome within a finished [SessionResult]'s [SessionResult.cardResults]. Present only
 * for a **Studied** card — a card drawn into the session and never reached simply has no entry;
 * there is no "unseen" outcome value. Studied differs by mode: Rated means at least one completed
 * Attempt (a silence timeout consumes no Attempt and does not qualify); Fast means the card's answer
 * was shown.
 *
 * Sealed by Study Mode, mirroring the persisted `sessions/{id}` document
 * ([ADR-0014](../../../../../../../docs/adr/0014-session-stats-written-at-summary-screen.md)):
 * [Rated] carries Attempts and the previously-mastered flag, [Fast] carries neither at all — not a
 * zeroed or falsed placeholder, genuinely absent from the type, because Fast has no Ratings, no
 * Attempts and no mastery concept to report. `state` stays the one field both variants share: both
 * modes do have a genuine outcome, just one each variant's mode can actually produce.
 *
 * Carries no transcript. A voice-answered card's sanitized transcript is shown on screen
 * transiently, during the session, to display grading feedback, and is discarded once the card is
 * graded — it never reaches this type.
 *
 * Private Flashcards must never appear in [SessionResult.cardResults] of either variant, and are
 * excluded from all progress accounting. The `Flashcard` model has no privacy field yet, so that
 * exclusion is recorded here as an invariant for whichever change introduces one — no field or
 * filter exists for it today.
 */
sealed interface FlashcardResult {
    val cardId: String
    val subcategoryId: String
    val state: FlashcardStudyProgressState

    /** @param wasPreviouslyMastered threaded from [RatedSessionCardRecord.wasPreviouslyMastered]. Spec 07's
     * Mastery Defense is what finally sets the upstream field to `true`; this type only carries it through. */
    data class Rated(
        override val cardId: String,
        override val subcategoryId: String,
        override val state: FlashcardStudyProgressState,
        val attemptsUsed: Int,
        val wasPreviouslyMastered: Boolean,
    ) : FlashcardResult

    /** Fast's only possible [state] is [FlashcardStudyProgressState.Seen] — there is no Attempts or mastery field to carry. */
    data class Fast(
        override val cardId: String,
        override val subcategoryId: String,
        override val state: FlashcardStudyProgressState,
    ) : FlashcardResult
}

/**
 * What happened in one Study Session of either [StudyMode] — the complete record handed to the
 * Session Summary screen, and the one shape everything downstream (spec 04's persistence, spec 05's
 * scoring) reads, rather than a type per mode living outside this hierarchy.
 *
 * Sealed by Study Mode, same reasoning as [FlashcardResult]: [Rated]'s Mastered/Partial/Failed counts
 * exist only on that variant, not as a zero on [Fast] — [Fast] structurally cannot tally outcomes it
 * has no Terminal States to produce. [mode] is deliberately not a stored field on either
 * variant — a stored `mode` alongside the sealed branch would be a second discriminant that could
 * disagree with the branch itself; it is derived from `this` instead, so there is exactly one source
 * of truth for which mode a result belongs to.
 */
sealed interface SessionResult {
    /** the session's identity, generated when the session starts. */
    val id: String

    /** when the first card was actually shown, not route entry — a session whose card load fails never banks time. */
    val startedAt: Instant

    /** an `Int`, matching [ADR-0014](../../../../../../../docs/adr/0014-session-stats-written-at-summary-screen.md)'s
     * persisted `durationSeconds` field. Measured by a [SessionClock]; rounded exactly once, there. */
    val durationSeconds: Int

    /** `true` when the session ended via exit confirmation before the deck was exhausted, `false` on a natural end. */
    val abandoned: Boolean
    val categoryId: String

    /** carried alongside its id so a stored session can be displayed later without a lookup (ADR-0014). */
    val categoryName: String
    val subcategoryIds: List<String>

    /** carried alongside their ids, same reason as [categoryName] (ADR-0014). */
    val subcategoryNames: List<String>

    /** one [FlashcardResult] per Studied card. */
    val cardResults: List<FlashcardResult>

    /** Derived from the sealed branch — see the type's own KDoc for why this is never a stored field. */
    val mode: StudyMode
        get() = when (this) {
            is Rated -> StudyMode.Rated
            is Fast -> StudyMode.Fast
        }

    /** How many cards were Studied. Both variants report this. */
    val studiedCount: Int get() = cardResults.size

    data class Rated(
        override val id: String,
        override val startedAt: Instant,
        override val durationSeconds: Int,
        override val abandoned: Boolean,
        override val categoryId: String,
        override val categoryName: String,
        override val subcategoryIds: List<String>,
        override val subcategoryNames: List<String>,
        override val cardResults: List<FlashcardResult.Rated>,
    ) : SessionResult {
        /**
         * The three Terminal State counts below are *derived* from [cardResults] rather than stored
         * alongside it, so they cannot disagree with it — this governs this in-memory type only.
         * ADR-0014's persisted `sessions/{id}` document separately stores its own such counts,
         * computed from this same list once, at commit time (spec 04); the two rules apply to
         * different layers and do not conflict.
         */
        val masteredCount: Int get() = cardResults.count { it.state == FlashcardStudyProgressState.Mastered }
        val partialCount: Int get() = cardResults.count { it.state == FlashcardStudyProgressState.Partial }
        val failedCount: Int get() = cardResults.count { it.state == FlashcardStudyProgressState.Failed }
    }

    data class Fast(
        override val id: String,
        override val startedAt: Instant,
        override val durationSeconds: Int,
        override val abandoned: Boolean,
        override val categoryId: String,
        override val categoryName: String,
        override val subcategoryIds: List<String>,
        override val subcategoryNames: List<String>,
        override val cardResults: List<FlashcardResult.Fast>,
    ) : SessionResult
}
