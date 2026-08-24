package com.rossomak.flashcards.feature.settings

/**
 * Everything the Settings screen's dialogs report back, behind a single
 * `onDialogEvent: (SettingsDialogEvent) -> Unit` parameter. Grouped into [Open] and [DraftChange]
 * like the study screens', so the three screens read the same way.
 *
 * [Confirm] and [Dismiss] carry no dialog identity: the ViewModel holds the open dialog and reads
 * its own draft.
 */
sealed interface SettingsDialogEvent {

    sealed interface Open : SettingsDialogEvent {

        data object VoiceSettings : Open
    }

    sealed interface DraftChange : SettingsDialogEvent {

        data class VoiceSettingsVoice(val voiceId: String?) : DraftChange

        data class VoiceSettingsSpeechRate(val speechRate: Float) : DraftChange
    }

    /**
     * Applies the draft. On this screen the change *is* permanent — there is no session to scope
     * it to and therefore no "Keep as my default" opt-in (ADR-0030).
     */
    data object Confirm : SettingsDialogEvent

    data object Dismiss : SettingsDialogEvent
}
