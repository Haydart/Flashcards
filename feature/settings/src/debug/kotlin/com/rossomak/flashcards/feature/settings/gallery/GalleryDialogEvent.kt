package com.rossomak.flashcards.feature.settings.gallery

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode

/**
 * Everything a dialog can report back, as one sealed type behind a single
 * `onDialogEvent: (GalleryDialogEvent) -> Unit` parameter.
 *
 * Wiring nine dialogs as individual lambdas would mean ~30 parameters on the content composable
 * and the same 30 no-op lambdas repeated in every preview. [Confirm] and [Dismiss] carry no
 * payload on purpose: the state owner already holds the draft in [GalleryDialog], so it reads its
 * own value rather than having it handed back.
 */
internal sealed interface GalleryDialogEvent {

    /** Applies the open dialog's draft. The only commit path. */
    data object Confirm : GalleryDialogEvent

    /** Throws the open dialog's draft away. Scrim tap, back press, or Cancel. */
    data object Dismiss : GalleryDialogEvent

    data class Open(val dialog: GalleryDialog) : GalleryDialogEvent

    data class SortDraftChange(val sortOrder: FlashcardSortOrder) : GalleryDialogEvent

    data class ModeDraftChange(val studyMode: StudyMode) : GalleryDialogEvent

    data class LengthDraftChange(val cardCount: Int) : GalleryDialogEvent

    data class VoiceAnsweringDraftChange(val enabled: Boolean) : GalleryDialogEvent

    data class VoiceDraftVoiceChange(val voiceId: String?) : GalleryDialogEvent

    data class VoiceDraftSpeechRateChange(val speechRate: Float) : GalleryDialogEvent

    data class KeepAsDefaultChange(val enabled: Boolean) : GalleryDialogEvent

    data class TagSelectedChange(val tag: String, val selected: Boolean) : GalleryDialogEvent

    data class DifficultyRangeChange(val range: IntRange) : GalleryDialogEvent

    data class ReportActionCheckedChange(
        val action: CurationAction,
        val checked: Boolean,
    ) : GalleryDialogEvent
}
