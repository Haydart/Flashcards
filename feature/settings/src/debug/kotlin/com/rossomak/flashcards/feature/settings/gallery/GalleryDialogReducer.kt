package com.rossomak.flashcards.feature.settings.gallery

import com.rossomak.flashcards.core.domain.model.CurationAction

/**
 * Applies a [GalleryDialogEvent] to the currently open dialog.
 *
 * This is the shape a ViewModel's `onDialogEvent` takes on a real screen: one entry point, one
 * `when`. Two things are worth copying from it — [GalleryDialogEvent.Dismiss] discards simply by
 * returning `null` (the draft dies with the field, so there is no discard logic to forget), and
 * every draft change narrows to its own dialog type via [update], so an event arriving for a
 * dialog that isn't open is a no-op rather than a crash.
 */
internal fun GalleryDialog?.reduce(event: GalleryDialogEvent): GalleryDialog? = when (event) {
    is GalleryDialogEvent.Open -> event.dialog

    // Both exits close the dialog. What differs is what the *caller* does about it: Confirm is
    // where a real screen would apply the draft and, if keepAsDefault is set, persist it.
    GalleryDialogEvent.Confirm, GalleryDialogEvent.Dismiss -> null

    is GalleryDialogEvent.SortDraftChange ->
        update<GalleryDialog.Sort> { it.copy(draft = event.sortOrder) }

    is GalleryDialogEvent.ModeDraftChange ->
        update<GalleryDialog.Mode> { it.copy(draft = event.studyMode) }

    is GalleryDialogEvent.LengthDraftChange ->
        update<GalleryDialog.Length> { it.copy(draft = event.cardCount) }

    is GalleryDialogEvent.VoiceAnsweringDraftChange ->
        update<GalleryDialog.VoiceAnswering> { it.copy(draft = event.enabled) }

    is GalleryDialogEvent.VoiceDraftVoiceChange ->
        update<GalleryDialog.Voice> { it.copy(draftVoiceId = event.voiceId) }

    is GalleryDialogEvent.VoiceDraftSpeechRateChange ->
        update<GalleryDialog.Voice> { it.copy(draftSpeechRate = event.speechRate) }

    is GalleryDialogEvent.TagSelectedChange -> update<GalleryDialog.Filters> { dialog ->
        val tags = if (event.selected) {
            dialog.draft.selectedTags + event.tag
        } else {
            dialog.draft.selectedTags - event.tag
        }
        dialog.copy(draft = dialog.draft.copy(selectedTags = tags))
    }

    is GalleryDialogEvent.DifficultyRangeChange -> update<GalleryDialog.Filters> { dialog ->
        dialog.copy(draft = dialog.draft.copy(difficultyRange = event.range))
    }

    is GalleryDialogEvent.ReportActionCheckedChange -> update<GalleryDialog.Report> { dialog ->
        dialog.copy(selectedActions = dialog.selectedActions.toggle(event.action, event.checked))
    }

    is GalleryDialogEvent.KeepAsDefaultChange -> when (this) {
        is GalleryDialog.Sort -> copy(keepAsDefault = event.enabled)
        is GalleryDialog.Mode -> copy(keepAsDefault = event.enabled)
        is GalleryDialog.Length -> copy(keepAsDefault = event.enabled)
        is GalleryDialog.VoiceAnswering -> copy(keepAsDefault = event.enabled)
        is GalleryDialog.Voice -> copy(keepAsDefault = event.enabled)
        else -> this
    }
}

/**
 * Checking one difficulty action clears the other: a card cannot be both too easy and too hard,
 * and [CurationAction.difficultyOpposite] already
 * encodes that pairing. The rows stay visually independent — the user just sees the opposite box
 * flip — but contradictory data never leaves the dialog.
 */
private fun Set<CurationAction>.toggle(
    action: CurationAction,
    checked: Boolean,
): Set<CurationAction> = when {
    !checked -> this - action
    else -> this - setOfNotNull(action.difficultyOpposite()) + action
}

/**
 * Narrowing helper — the one cost of keeping dialog state in a single sealed field. Paid once
 * here instead of at every call site.
 */
private inline fun <reified T : GalleryDialog> GalleryDialog?.update(transform: (T) -> T): GalleryDialog? =
    (this as? T)?.let(transform) ?: this
