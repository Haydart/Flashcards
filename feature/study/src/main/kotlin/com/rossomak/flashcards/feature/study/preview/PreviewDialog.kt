package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters

/**
 * Which dialog the Preview screen currently has open, and the draft it is editing.
 *
 * One sealed nullable field rather than a flag per dialog: two dialogs open at once becomes
 * unrepresentable, the call site is a single exhaustive `when` (so a new dialog cannot be added
 * without being wired), and **discard on dismiss is free** — the draft dies with the field.
 *
 * A draft holds a domain value plus UI-only companions. It is UI state and never becomes a domain
 * model; [StudySessionConfig][com.rossomak.flashcards.core.domain.model.StudySessionConfig] is
 * what a confirmed draft folds into.
 */
sealed interface PreviewDialog {

    data class Mode(val draft: StudyMode, val keepAsDefault: Boolean = false) : PreviewDialog

    data class VoiceAnswering(val draft: Boolean, val keepAsDefault: Boolean = false) : PreviewDialog

    data class Length(val draft: Int, val keepAsDefault: Boolean = false) : PreviewDialog

    data class Sort(val draft: FlashcardSortOrder, val keepAsDefault: Boolean = false) : PreviewDialog

    /**
     * No `keepAsDefault`: tags belong to one subcategory and cannot carry to another, so filters
     * are session-scoped by definition (ADR-0030).
     */
    data class Filters(val draft: FlashcardFilters) : PreviewDialog
}
