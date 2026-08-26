package com.rossomak.flashcards.core.domain.model

/**
 * Bounds of the Daily Goal — the user's target for minutes studied per calendar day. Lives in the
 * domain rather than in the onboarding UI because the same range backs the Settings and Progress
 * editors of the same value.
 */
object DailyGoal {
    /** Applied when the user never sets a goal, including when they skip onboarding. */
    const val DEFAULT_MINUTES = 20

    const val MIN_MINUTES = 5

    const val MAX_MINUTES = 120

    /** Granularity of every stepper/slider that edits the goal. */
    const val STEP_MINUTES = 5

    /** Clamps [minutes] into [MIN_MINUTES]..[MAX_MINUTES]. */
    fun coerce(minutes: Int): Int = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
}
