package com.rossomak.flashcards.core.domain.model

/**
 * Everything that decides which cards a study session draws and how it plays them back.
 *
 * Session-scoped by construction. A user's *defaults* are a different, partial type — filters
 * ([tagIds], [difficultyRange]) are never a default because tags belong to one subcategory and
 * cannot carry to another (ADR-0030).
 *
 * @param ratedAttempts how many times a card may be answered before it counts as failed. Rated
 * only — Fast mode has no rating step to retry (ADR-0025) — so it is carried but ignored there
 * rather than being made nullable.
 * @param readAloudEnabled Fast-mode auto-play: answers are spoken and cards advance hands-free.
 * The Fast counterpart of [voiceAnsweringEnabled], and ignored in Rated mode for the same reason.
 * @param partialRatingCardRequeueingEnabled Rated-only: whether a Partial rating re-queues the card
 * (`true`, the default) or finishes it on the spot (`false`). A card finished this way records
 * Terminal Partial, not Mastered (ADR-0044). Session-scoped like every other field here — the
 * Preview screen may set it for one session without keeping it as a default.
 * @param tagIds OR-within: a card matches if it carries any of them. Empty means "no tag filter".
 * @param difficultyRange AND-combined with [tagIds].
 * @param subcategoryCountRange how many Subcategories a Quick Session samples its pool from
 * (ADR-0040). Carried unconditionally and ignored outside Quick — same idiom as [ratedAttempts] in
 * Fast mode — since single-Subcategory and Custom sessions hand a fixed, already-resolved
 * Subcategory list to selection.
 */
data class StudySessionConfig(
    val subcategoryIds: List<String>,
    val mode: StudyMode = StudyMode.Rated,
    val voiceAnsweringEnabled: Boolean = false,
    val ratedAttempts: Int = DEFAULT_RATED_ATTEMPTS,
    val readAloudEnabled: Boolean = false,
    val partialRatingCardRequeueingEnabled: Boolean = true,
    val length: Int = DEFAULT_LENGTH,
    val sortOrder: FlashcardSortOrder = FlashcardSortOrder.Default,
    val voiceSettings: VoiceSettings = VoiceSettings(),
    val difficultyRange: IntRange = MIN_DIFFICULTY..MAX_DIFFICULTY,
    val tagIds: Set<String> = emptySet(),
    val subcategoryCountRange: IntRange = DEFAULT_SUBCATEGORY_COUNT_RANGE,
) {

    companion object {
        const val MIN_LENGTH = 10
        const val MAX_LENGTH = 50
        const val LENGTH_STEP = 5
        const val DEFAULT_LENGTH = 20
        const val MIN_DIFFICULTY = 1
        const val MAX_DIFFICULTY = 10
        const val MIN_SUBCATEGORY_COUNT = 1
        const val MAX_SUBCATEGORY_COUNT = 5
        val DEFAULT_SUBCATEGORY_COUNT_RANGE = 3..5

        /**
         * One attempt means no retry — a legitimate strict setting, not a broken state. The
         * ceiling is where the per-card attempt indicator stops being readable at a glance, not a
         * claim about learning.
         */
        const val MIN_RATED_ATTEMPTS = 1
        const val MAX_RATED_ATTEMPTS = 5
        const val RATED_ATTEMPTS_STEP = 1
        const val DEFAULT_RATED_ATTEMPTS = 3

        /**
         * [RatedSessionState]'s re-insertion gap for a Failed Rating, in cards shown before this
         * one returns — never 0 or 1, so the card is a recall rather than a re-read of the answer
         * still on screen ([ADR-0046](../../../../../../../docs/adr/0046-failed-and-partial-re-insertion-placement.md)).
         */
        const val FAILED_REQUEUE_MIN_GAP = 2
        const val FAILED_REQUEUE_MAX_GAP = 4

        /** [RatedSessionState]'s re-insertion gap for a Partial Rating — further out than Failed. */
        const val PARTIAL_REQUEUE_MIN_GAP = 5
        const val PARTIAL_REQUEUE_MAX_GAP = 9
    }
}
