package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.StudyMode
import kotlinx.serialization.Serializable

@Serializable
data class PreviewStudySessionRoute(
    val categoryId: String,
    val categoryName: String,
    val subcategoryIds: List<String>,
    val subcategoryNames: List<String>,
    val filterTagIds: List<String> = emptyList(),
    val isQuickSession: Boolean = false,
)

/**
 * @param voiceAnsweringEnabled the Preview screen's choice (ADR-0030). Honoured on entry for
 * Rated sessions only — Fast mode has no rating step for voice answering to drive (ADR-0025).
 */
@Serializable
data class StudySessionRoute(
    val categoryId: String,
    val sessionTitle: String,
    val subcategoryIds: List<String>,
    val cardIds: List<String>,
    val studyMode: StudyMode,
    val voiceAnsweringEnabled: Boolean = false,
)

@Serializable
data object StudySummaryRoute
