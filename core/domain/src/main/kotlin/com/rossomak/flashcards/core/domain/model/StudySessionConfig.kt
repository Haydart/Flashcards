package com.rossomak.flashcards.core.domain.model

/**
 * Everything that decides which cards a study session draws and how it plays them back.
 *
 * Session-scoped by construction. A user's *defaults* are a different, partial type — filters
 * ([tagIds], [difficultyRange]) are never a default because tags belong to one subcategory and
 * cannot carry to another (ADR-0030).
 *
 * Selection is a pure deterministic function of this value: same config in, same cards out. That
 * is what [seed] buys — re-randomizing is `copy(seed = Random.nextLong())` and a test pins it.
 *
 * > **Do not exclude [seed] from `equals()`.** `MutableStateFlow` conflates by equality, so a
 * > config that ignored the seed would make re-randomize emit nothing and silently do nothing.
 * > A comparison that genuinely needs to ignore it belongs in a named function, not in `equals`.
 *
 * @param ratedAttempts how many times a card may be answered before it counts as failed. Rated
 * only — Fast mode has no rating step to retry (ADR-0025) — so it is carried but ignored there
 * rather than being made nullable.
 * @param readAloudEnabled Fast-mode auto-play: answers are spoken and cards advance hands-free.
 * The Fast counterpart of [voiceAnsweringEnabled], and ignored in Rated mode for the same reason.
 * @param tagIds OR-within: a card matches if it carries any of them. Empty means "no tag filter".
 * @param difficultyRange AND-combined with [tagIds].
 */
data class StudySessionConfig(
    val subcategoryIds: List<String>,
    val mode: StudyMode = StudyMode.Rated,
    val voiceAnsweringEnabled: Boolean = false,
    val ratedAttempts: Int = DEFAULT_RATED_ATTEMPTS,
    val readAloudEnabled: Boolean = false,
    val length: Int = DEFAULT_LENGTH,
    val sortOrder: FlashcardSortOrder = FlashcardSortOrder.Default,
    val difficultyRange: IntRange = MIN_DIFFICULTY..MAX_DIFFICULTY,
    val tagIds: Set<String> = emptySet(),
    val seed: Long = 0L,
) {

    companion object {
        const val MIN_LENGTH = 10
        const val MAX_LENGTH = 50
        const val LENGTH_STEP = 5
        const val DEFAULT_LENGTH = 20
        const val MIN_DIFFICULTY = 1
        const val MAX_DIFFICULTY = 10

        /**
         * One attempt means no retry — a legitimate strict setting, not a broken state. The
         * ceiling is where the per-card attempt indicator stops being readable at a glance, not a
         * claim about learning.
         */
        const val MIN_RATED_ATTEMPTS = 1
        const val MAX_RATED_ATTEMPTS = 5
        const val RATED_ATTEMPTS_STEP = 1
        const val DEFAULT_RATED_ATTEMPTS = 3
    }
}
