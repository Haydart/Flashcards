package com.rossomak.flashcards.feature.study.session

import com.rossomak.flashcards.core.domain.model.CurationAction

/**
 * Everything the study session's dialogs report back, behind a single
 * `onDialogEvent: (StudySessionDialogEvent) -> Unit` parameter — the same shape the Preview screen
 * uses, for the same reason: five dialogs wired as individual lambdas would push an already
 * baseline-suppressed parameter list further past detekt's threshold.
 *
 * [Confirm] and [Dismiss] carry no dialog identity: the ViewModel holds the open dialog and reads
 * its own draft.
 */
sealed interface StudySessionDialogEvent {

    sealed interface Open : StudySessionDialogEvent {

        data object ReportProblem : Open

        data object ExtendedContext : Open

        data object VoiceSettings : Open

        data object ExitSession : Open
    }

    /** An edit inside an open dialog. Nothing is applied until [Confirm]. */
    sealed interface DraftChange : StudySessionDialogEvent {

        data class ReportProblemAction(val action: CurationAction, val isChecked: Boolean) : DraftChange

        data class VoiceSettingsVoice(val voiceId: String?) : DraftChange

        data class VoiceSettingsSpeechRate(val speechRate: Float) : DraftChange
    }

    /** The action button: submits the report, saves voice settings, accepts consent. */
    data object Confirm : StudySessionDialogEvent

    /** Scrim, back press, or Cancel — always the discard path. */
    data object Dismiss : StudySessionDialogEvent
}
