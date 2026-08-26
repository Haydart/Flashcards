package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.domain.model.StudySessionConfig

data class PreviewStudySessionScreenState(
    val categoryName: String = "",
    val subcategoryNames: List<String> = emptyList(),
    val isQuickSession: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val config: StudySessionConfig = StudySessionConfig(subcategoryIds = emptyList()),
    val selectedCardCount: Int = 0,
    val estimatedMinutes: Int = 0,
    /**
     * Tag vocabulary of the pool, offered by the Filters dialog. Empty for multi-subcategory
     * sessions, which filter by difficulty only (ADR-0030).
     */
    val availableTags: List<String> = emptyList(),
    val activeDialog: PreviewDialog? = null,
) {
    val isSingleTopic: Boolean get() = subcategoryNames.size == 1
    val topicCount: Int get() = subcategoryNames.size
    val canRerandomize: Boolean get() = !isSingleTopic || isQuickSession
    val canStart: Boolean get() = !isLoading && error == null && selectedCardCount > 0
}
