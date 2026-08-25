package com.rossomak.flashcards.core.ui.dialog

/**
 * Everything a screen's dialogs report back, behind a single
 * `onDialogEvent: (XxxDialogEvent) -> Unit` parameter (ADR-0036).
 *
 * Shared by every screen, because none of it is screen-specific: what varies is only the sealed
 * dialog type [D], which each screen names with a typealias so the parameter never appears at a
 * call site:
 *
 * ```kotlin
 * typealias PreviewDialogEvent = DialogEvent<PreviewDialog>
 * ```
 *
 * There is no separate list of openable dialogs. [D] already states which dialogs a screen has and
 * what data each carries, and restating that as a parallel hierarchy of `Open` cases would be a
 * mirror kept in sync by hand.
 *
 * @param D the screen's sealed dialog, which doubles as its draft state carrier.
 */
sealed interface DialogEvent<out D> {

    /**
     * A row was tapped, carrying the dialog to show. The caller seeds the draft: a row that opens a
     * dialog is already rendering the committed value it seeds from, so nothing has to be threaded
     * in for it. A dialog whose seed lives outside screen state — voice settings, whose draft comes
     * from the shared controller — is re-seeded by the ViewModel.
     */
    data class Open<out D>(val dialog: D) : DialogEvent<D>

    /**
     * The next draft, built by the host from the dialog on screen. The host's `when` has already
     * narrowed to a concrete case, so it emits a total `copy()` — no per-field event, no cast.
     *
     * Distinct from [Open] despite carrying the same type: a ViewModel has to tell an edit from an
     * open to fire the right side effect.
     */
    data class DraftChange<out D>(val dialog: D) : DialogEvent<D>

    /** Applies the draft. The only commit path — dismissing discards it. */
    data object Confirm : DialogEvent<Nothing>

    /** Scrim, back press, or Cancel — always the discard path. */
    data object Dismiss : DialogEvent<Nothing>
}
