package com.rossomak.flashcards.feature.study.session

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.ui.dialog.DialogEvent
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState

/** The event type this screen's dialogs report through. See [DialogEvent]. */
typealias StudySessionDialogEvent = DialogEvent<StudySessionDialog>

/**
 * Which dialog the study session currently has open, and everything that dialog needs.
 *
 * One sealed nullable field replaces the three independent flags this screen used to carry
 * (`isCurationDialogVisible`, `isVoiceAnswerConsentDialogVisible`, and a composable-local
 * `remember` for the extended-context dialog, which duplicated a ViewModel flag of its own). Two
 * dialogs open at once is now unrepresentable, and dismissal discards a draft for free.
 *
 * This is the screen's whole dialog contract: the set below is what it can show, and each case
 * states what that dialog carries. Nothing restates the set — opening one means handing over an
 * instance from here, so there is no parallel hierarchy to keep in sync.
 *
 * A case carries its draft plus any context needed to render it, so [StudySessionDialogHost] takes
 * no parameter per dialog (ADR-0036).
 */
sealed interface StudySessionDialog {

    /**
     * The report draft. Always starts empty: this files a fresh report rather than editing the
     * card's previous one, so an unchecked row is never ambiguous between "not a problem" and
     * "already reported" (ADR-0017).
     */
    data class ReportProblem(
        val cardId: String,
        val subcategoryId: String,
        val selectedActions: Set<CurationAction> = emptySet(),
    ) : StudySessionDialog {
        val canSubmit: Boolean get() = selectedActions.isNotEmpty()

        /**
         * Rows toggle independently, but "too easy" and "too hard" contradict each other, so
         * checking one clears the other — contradictory data never reaches the repository.
         *
         * Lives on the draft rather than in the host because a host may only do total field-level
         * `copy()` (ADR-0036); this branches, so it needs somewhere a unit test can reach it.
         */
        fun withAction(action: CurationAction, isChecked: Boolean): ReportProblem = copy(
            selectedActions = if (isChecked) {
                selectedActions + action - setOfNotNull(action.difficultyOpposite())
            } else {
                selectedActions - action
            },
        )
    }

    /**
     * Reads the current card's extended context, seeded at open; the card itself stays in the card
     * list. A card with nothing to show never opens this.
     */
    data class ExtendedContext(val text: String) : StudySessionDialog

    /** Raised by the voice-answering flow rather than by a tap, so nothing seeds it. */
    data object VoiceAnswerConsent : StudySessionDialog

    /**
     * The draft lives here like every other dialog's, but is the one this screen cannot seed at the
     * call site: it comes from
     * [VoiceSettingsController][com.rossomak.flashcards.core.ui.voice.VoiceSettingsController]'s
     * saved settings and voice cache, which the toolbar does not have. The ViewModel always
     * replaces what it is handed, so the default here is a placeholder, never a value in use.
     */
    data class VoiceSettings(val draft: VoiceSettingsDraftState = VoiceSettingsDraftState()) : StudySessionDialog

    data object ExitSession : StudySessionDialog
}
