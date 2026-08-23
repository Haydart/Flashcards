package com.rossomak.flashcards.feature.settings.gallery

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters

/**
 * Which dialog the gallery currently has open, and its in-flight draft.
 *
 * This is the real screen-state shape the dialog system expects: **one** sealed nullable field
 * rather than a visibility boolean per dialog. Two dialogs open at once is unrepresentable,
 * discarding a draft on dismiss is free (the draft dies with the field), and the host's `when` is
 * exhaustive, so a new dialog will not compile until it is rendered.
 *
 * On a production screen this lives in the ViewModel-owned screen state. The gallery keeps it in a
 * `remember` because it has no ViewModel and no persistence.
 */
internal sealed interface GalleryDialog {

    data class Sort(
        val draft: FlashcardSortOrder,
        val keepAsDefault: Boolean = false,
    ) : GalleryDialog

    data class Mode(
        val draft: StudyMode,
        val keepAsDefault: Boolean = false,
    ) : GalleryDialog

    data class Length(
        val draft: Int,
        val keepAsDefault: Boolean = false,
    ) : GalleryDialog

    data class VoiceAnswering(
        val draft: Boolean,
        val keepAsDefault: Boolean = false,
    ) : GalleryDialog

    data class Voice(
        val draftVoiceId: String?,
        val draftSpeechRate: Float,
        val availableVoices: List<VoiceOption>,
        val keepAsDefault: Boolean = false,
    ) : GalleryDialog

    /** No `keepAsDefault` — filters are always session-scoped (ADR-0030). */
    data class Filters(val draft: FlashcardFilters) : GalleryDialog

    data class Report(val selectedActions: Set<CurationAction>) : GalleryDialog

    data object ExtendedContext : GalleryDialog

    data object ExitSession : GalleryDialog
}
