package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.ui.navigation.NavigationEvent
import com.rossomak.flashcards.feature.study.StudySessionRoute

sealed interface PreviewStudySessionDestination : NavigationEvent {
    data class StudySession(val route: StudySessionRoute) : PreviewStudySessionDestination
}
