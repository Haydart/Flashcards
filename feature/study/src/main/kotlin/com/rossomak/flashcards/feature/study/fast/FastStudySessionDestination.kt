package com.rossomak.flashcards.feature.study.fast

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

/**
 * Where a Fast Study Session can send the user. One-time events rather than state (ADR-0019):
 * leaving is a transition, and a flag in state would re-fire it on every recomposition after the
 * fact.
 */
sealed interface FastStudySessionDestination : NavigationEvent {

    /** The session ended — either the last card was advanced past, or the user confirmed "Exit session?". */
    data object Back : FastStudySessionDestination
}
