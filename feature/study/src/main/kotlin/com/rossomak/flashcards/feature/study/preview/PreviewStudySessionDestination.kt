package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent
import com.rossomak.flashcards.feature.study.FastStudySessionRoute
import com.rossomak.flashcards.feature.study.RatedStudySessionRoute

sealed interface PreviewStudySessionDestination : NavigationEvent {

    /** Chosen when the confirmed Study Mode is Fast (ADR-0045). */
    data class FastStudySession(val route: FastStudySessionRoute) : PreviewStudySessionDestination

    /** Chosen when the confirmed Study Mode is Rated (ADR-0045). */
    data class RatedStudySession(val route: RatedStudySessionRoute) : PreviewStudySessionDestination
}
