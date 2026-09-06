package com.rossomak.flashcards.core.domain.model

import java.time.Instant

/**
 * One card's outcome within a finished [SessionResult]'s ledger. Present only for a **Studied**
 * card — a card drawn into the session and never reached simply has no entry; there is no "unseen"
 * outcome value. Studied differs by mode: Rated means at least one completed Attempt (a silence
 * timeout consumes no Attempt and does not qualify); Fast means the card's answer was shown.
 *
 * Carries no transcript. A voice-answered card's sanitized transcript is shown on screen
 * transiently, during the session, to display grading feedback, and is discarded once the card is
 * graded — it never reaches this type.
 *
 * Private Flashcards must never appear in a ledger of either mode, and are excluded from all
 * progress accounting. The `Flashcard` model has no privacy field yet, so that exclusion is recorded
 * here as an invariant for whichever change introduces one — no field or filter exists for it today.
 *
 * @param wasPreviouslyMastered threaded from [RatedSessionCardRecord.wasPreviouslyMastered] for a
 * Rated card, always `false` for a Fast one — Fast has no such record to read one from. Spec 07's
 * Mastery Defense is what finally sets the upstream field to `true`; this type only carries it
 * through.
 */
data class SessionLedgerEntry(
    val cardId: String,
    val subcategoryId: String,
    val state: FlashcardProgressState,
    val attemptsUsed: Int,
    val wasPreviouslyMastered: Boolean,
)

/**
 * What happened in one Study Session of either [StudyMode] — the complete record handed to the
 * Session Summary screen, and the one shape everything downstream (spec 04's persistence, spec 05's
 * scoring) reads, rather than a type per mode.
 *
 * @param id the session's identity, generated when the session starts.
 * @param startedAt when the first card was actually shown, not route entry — a session whose card
 * load fails never banks time.
 * @param durationSeconds an `Int`, matching [ADR-0014](../../../../../../../docs/adr/0014-session-stats-written-at-summary-screen.md)'s
 * persisted `durationSeconds` field. Measured by a [SessionClock]; rounded exactly once, there.
 * @param abandoned `true` when the session ended via exit confirmation before the deck was
 * exhausted, `false` on a natural end.
 * @param categoryName and [subcategoryNames] are carried alongside their ids so a stored session
 * can be displayed later without a lookup (ADR-0014).
 * @param ledger one [SessionLedgerEntry] per Studied card. The Mastered/Partial/Failed counts below
 * are *derived* from it rather than stored alongside it, so they cannot disagree with it — this
 * governs this in-memory type only. ADR-0014's persisted `sessions/{id}` document separately stores
 * its own such counts, computed from this same ledger once, at commit time (spec 04); the two rules
 * apply to different layers and do not conflict.
 */
data class SessionResult(
    val id: String,
    val mode: StudyMode,
    val startedAt: Instant,
    val durationSeconds: Int,
    val abandoned: Boolean,
    val categoryId: String,
    val categoryName: String,
    val subcategoryIds: List<String>,
    val subcategoryNames: List<String>,
    val ledger: List<SessionLedgerEntry>,
) {
    /** How many cards were Studied. Both modes report this. */
    val studiedCount: Int get() = ledger.size

    /** Always 0 for a Fast result — Fast never produces [FlashcardProgressState.Mastered]. */
    val masteredCount: Int get() = ledger.count { it.state == FlashcardProgressState.Mastered }

    /** Always 0 for a Fast result — Fast never produces [FlashcardProgressState.Partial]. */
    val partialCount: Int get() = ledger.count { it.state == FlashcardProgressState.Partial }

    /** Always 0 for a Fast result — Fast never produces [FlashcardProgressState.Failed]. */
    val failedCount: Int get() = ledger.count { it.state == FlashcardProgressState.Failed }
}
