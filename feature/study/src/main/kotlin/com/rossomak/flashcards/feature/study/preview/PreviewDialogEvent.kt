package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder

/**
 * Everything the Preview screen's dialogs report back, behind a single
 * `onDialogEvent: (PreviewDialogEvent) -> Unit` parameter.
 *
 * Only the sort dialog exists here so far. The screen will grow Mode, Length, Voice answering,
 * Voice/TTS and Filters dialogs, and each adds cases here rather than three more lambdas to
 * `PreviewStudySessionContent` — which is already past detekt's parameter threshold.
 *
 * [Confirm] and [Dismiss] carry no payload on purpose: the ViewModel already holds the draft, so
 * it reads its own value instead of having it handed back.
 */
sealed interface PreviewDialogEvent {

    data object SortOpen : PreviewDialogEvent

    /** Applies the draft. The only commit path — dismissing discards it. */
    data object Confirm : PreviewDialogEvent

    data object Dismiss : PreviewDialogEvent

    data class SortDraftChange(val sortOrder: FlashcardSortOrder) : PreviewDialogEvent

    data class KeepAsDefaultChange(val enabled: Boolean) : PreviewDialogEvent
}
