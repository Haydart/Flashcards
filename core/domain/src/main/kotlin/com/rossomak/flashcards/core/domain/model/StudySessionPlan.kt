package com.rossomak.flashcards.core.domain.model

/**
 * The cards a [StudySessionConfig] resolves to, how long working through them is expected to take,
 * and the tag vocabulary of the pool they were drawn from. The estimate is domain arithmetic over
 * a domain constant, so it is computed here rather than re-derived by each screen that shows it.
 */
data class StudySessionPlan(
    val cards: List<Flashcard>,
    val estimatedMinutes: Int,
    val poolTags: List<String>,
) {

    companion object {
        /** Rough pace of a Rated card: read, think, reveal, rate. */
        const val SECONDS_PER_CARD = 40
        private const val SECONDS_PER_MINUTE = 60

        /**
         * Rounds the estimate up, so a plan with any cards at all never reads as "~0 min".
         *
         * [poolTags] comes from the whole pool rather than from [cards]: it is what a filter UI
         * offers as options, and deriving it from the drawn cards would make tags disappear from
         * the picker precisely because the user filtered them out.
         */
        fun of(cards: List<Flashcard>, pool: List<Flashcard>): StudySessionPlan = StudySessionPlan(
            cards = cards,
            estimatedMinutes = ((cards.size * SECONDS_PER_CARD) + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE,
            poolTags = pool.flatMap { it.tags }.distinct().sorted(),
        )
    }
}
