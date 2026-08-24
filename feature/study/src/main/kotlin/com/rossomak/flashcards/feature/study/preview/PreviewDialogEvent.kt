package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode

/**
 * Everything the Preview screen's dialogs report back, behind a single
 * `onDialogEvent: (PreviewDialogEvent) -> Unit` parameter.
 *
 * Five dialogs wired as individual lambdas would be roughly twenty parameters on the content
 * composable and twenty no-op lambdas in every `@Preview`, well past detekt's threshold. Precedent
 * for the sealed shape: ADR-0019's one-time navigation events.
 *
 * Grouped into [Open] and [DraftChange] rather than left flat so the ViewModel can dispatch in
 * three small exhaustive `when`s instead of one large one — exhaustiveness is the point of the
 * sealed hierarchy, so it is never traded away for an `else`.
 *
 * [Confirm] and [Dismiss] carry no dialog identity: the ViewModel already holds the open dialog
 * and its draft, so it reads its own value rather than having it handed back.
 */
sealed interface PreviewDialogEvent {

    /** A row was tapped. The ViewModel seeds the draft from the committed config. */
    sealed interface Open : PreviewDialogEvent {

        data object Mode : Open

        data object VoiceAnswering : Open

        data object Length : Open

        data object Filters : Open

        data object Sort : Open
    }

    /** An edit inside an open dialog. Nothing is applied until [Confirm]. */
    sealed interface DraftChange : PreviewDialogEvent {

        data class Mode(val mode: StudyMode) : DraftChange

        data class VoiceAnswering(val isEnabled: Boolean) : DraftChange

        data class Length(val length: Int) : DraftChange

        data class SortOrder(val sortOrder: FlashcardSortOrder) : DraftChange

        data class FilterTag(val tag: String, val isSelected: Boolean) : DraftChange

        data class FilterDifficulty(val difficultyRange: IntRange) : DraftChange

        /** Just another draft field, applied atomically with the rest on [Confirm]. */
        data class KeepAsDefault(val isEnabled: Boolean) : DraftChange
    }

    /** Applies the draft. The only commit path — dismissing discards it. */
    data object Confirm : PreviewDialogEvent

    data object Dismiss : PreviewDialogEvent
}
