package com.rossomak.flashcards.feature.study.fast

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent
import com.rossomak.flashcards.feature.study.StudySessionSummaryRoute

/**
 * Where a Fast Study Session can send the user. One-time events rather than state (ADR-0019):
 * leaving is a transition, and a flag in state would re-fire it on every recomposition after the
 * fact.
 */
sealed interface FastStudySessionDestination : NavigationEvent {

    /**
     * The session ended — the deck was exhausted, or the user confirmed "Exit session?" — carrying
     * the sealed result on to the Session Summary. The session's only
     * destination now; there is no longer a plain "go back" outcome.
     */
    data class Summary(val route: StudySessionSummaryRoute) : FastStudySessionDestination
}
