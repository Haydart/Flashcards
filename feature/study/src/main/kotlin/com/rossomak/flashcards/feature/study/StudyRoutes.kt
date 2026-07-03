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

@Serializable
data class StudySessionRoute(
    val categoryId: String,
    val sessionTitle: String,
    val subcategoryIds: List<String>,
    val cardIds: List<String>,
    val studyMode: StudyMode,
)

@Serializable
data object StudySummaryRoute
