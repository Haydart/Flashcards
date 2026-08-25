package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters
import com.rossomak.flashcards.core.ui.dialog.DialogEvent

/** The event type this screen's dialogs report through. See [DialogEvent]. */
typealias PreviewDialogEvent = DialogEvent<PreviewDialog>

/**
 * Which dialog the Preview screen currently has open, and everything that dialog needs.
 *
 * This is the screen's whole dialog contract: the set below is what it can show, and each case
 * states what that dialog carries. Nothing restates the set — opening one means handing over an
 * instance from here, so there is no parallel hierarchy to keep in sync.
 *
 * One sealed nullable field rather than a flag per dialog: two dialogs open at once becomes
 * unrepresentable, the call site is a single exhaustive `when` (so a new dialog cannot be added
 * without being wired), and **discard on dismiss is free** — the draft dies with the field.
 *
 * A case carries its draft plus any context needed to render it, so [PreviewDialogHost] takes
 * nothing but the open dialog and the callback. A draft holds a domain value plus UI-only
 * companions. It is UI state and never becomes a domain model;
 * [StudySessionConfig][com.rossomak.flashcards.core.domain.model.StudySessionConfig] is what a
 * confirmed draft folds into.
 */
sealed interface PreviewDialog {

    data class Mode(val draft: StudyMode, val keepAsDefault: Boolean = false) : PreviewDialog

    data class VoiceAnswering(val draft: Boolean, val keepAsDefault: Boolean = false) : PreviewDialog

    data class Length(val draft: Int, val keepAsDefault: Boolean = false) : PreviewDialog

    data class Sort(val draft: FlashcardSortOrder, val keepAsDefault: Boolean = false) : PreviewDialog

    /**
     * No `keepAsDefault` option: tags belong to one subcategory and cannot carry to another, so filters
     * are session-scoped by definition (ADR-0030).
     * [availableTags] is the pool's tag vocabulary
     */
    data class Filters(val draft: FlashcardFilters, val availableTags: List<String>) : PreviewDialog
}
