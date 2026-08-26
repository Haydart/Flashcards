package com.rossomak.flashcards.feature.onboarding

/**
 * The eight pages of the onboarding pager, in order. All eight are ordinary pages: the flow has no
 * one-way doors, so [Welcome] can be swiped back to and [AllSet] can be swiped away from. The two
 * bookends simply carry no progress bar, and [AllSet] carries no Skip — chrome visibility is a
 * function of the settled step, nothing more.
 */
enum class OnboardingStep {
    Welcome,
    Structure,
    Mastery,
    SessionModes,
    DailyGoal,
    VoicePrivacy,
    Favorites,
    AllSet,
    ;

    /** Whether the top bar offers Skip. The final step is the flow's only exit, so it does not. */
    val showsSkip: Boolean
        get() = this != AllSet

    /**
     * Position of this step within the progress bar, or `null` for the bookends that show none.
     * Six of the eight pages are steps; [Welcome] and [AllSet] are a cover and an outro.
     */
    val progressIndex: Int?
        get() = when (this) {
            Welcome, AllSet -> null
            else -> ordinal - 1
        }

    companion object {
        /** Number of segments in the progress bar — every step except the two bookends. */
        val PROGRESS_SEGMENT_COUNT: Int = entries.count { it.progressIndex != null }

        val LAST: OnboardingStep = entries.last()

        fun atPage(page: Int): OnboardingStep = entries[page.coerceIn(entries.indices)]
    }
}
