package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.domain.model.CardSortOrder
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
    val selectedStudyMode: StudyMode = StudyMode.Rated,
    val sessionCardCount: Int = DEFAULT_SESSION_CARD_COUNT,
    val difficultyRange: IntRange = MIN_DIFFICULTY..MAX_DIFFICULTY,
    val sortOrder: CardSortOrder = CardSortOrder.Default,
    val isSortDialogVisible: Boolean = false,
) {
    val isSingleTopic: Boolean get() = subcategoryNames.size == 1
    val topicCount: Int get() = subcategoryNames.size
    val canRerandomize: Boolean get() = !isSingleTopic || isQuickSession
    val canStart: Boolean get() = !isLoading && error == null && selectedCardCount > 0

    companion object {
        const val MIN_SESSION_CARD_COUNT = 10
        const val MAX_SESSION_CARD_COUNT = 50
        const val DEFAULT_SESSION_CARD_COUNT = 20
        const val MIN_DIFFICULTY = 1
        const val MAX_DIFFICULTY = 10
    }
}
