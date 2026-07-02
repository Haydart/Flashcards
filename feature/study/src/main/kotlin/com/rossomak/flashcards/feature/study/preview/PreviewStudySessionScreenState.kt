package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.domain.model.StudyMode

data class PreviewStudySessionScreenState(
    val categoryName: String = "",
    val subcategoryNames: List<String> = emptyList(),
    val isQuickSession: Boolean = false,
    val filterTags: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedCardCount: Int = 0,
    val estimatedMinutes: Int = 0,
    val selectedStudyMode: StudyMode = StudyMode.RATED,
) {
    val isSingleTopic: Boolean get() = subcategoryNames.size == 1
    val topicCount: Int get() = subcategoryNames.size
    val canRerandomize: Boolean get() = !isSingleTopic || isQuickSession
    val canStart: Boolean get() = !isLoading && error == null && selectedCardCount > 0
}
