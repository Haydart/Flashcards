package com.rossomak.flashcards.feature.study.session

import com.rossomak.flashcards.core.domain.model.CurationAction

/**
 * Which dialog the study session currently has open, and the draft it is editing.
 *
 * One sealed nullable field replaces the three independent flags this screen used to carry
 * (`isCurationDialogVisible`, `isVoiceAnswerConsentDialogVisible`, and a composable-local
 * `remember` for the extended-context dialog, which duplicated a ViewModel flag of its own). Two
 * dialogs open at once is now unrepresentable, and dismissal discards a draft for free.
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
    }

    /** Reads the current card's extended context; the card itself stays in the card list. */
    data object ExtendedContext : StudySessionDialog

    data object VoiceAnswerConsent : StudySessionDialog

    data object VoiceSettings : StudySessionDialog

    data object ExitSession : StudySessionDialog
}
