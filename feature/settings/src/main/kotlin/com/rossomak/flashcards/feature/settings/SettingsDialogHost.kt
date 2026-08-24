package com.rossomak.flashcards.feature.settings

import androidx.compose.runtime.Composable
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceSettingsDialog
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState

/** Renders whichever dialog [activeDialog] names, or nothing when it is `null`. */
@Composable
internal fun SettingsDialogHost(
    activeDialog: SettingsDialog?,
    voiceSettingsState: VoiceSettingsDraftState,
    onDialogEvent: (SettingsDialogEvent) -> Unit,
) {
    when (activeDialog) {
        null -> Unit

        SettingsDialog.VoiceSettings -> VoiceSettingsDialog(
            availableVoices = voiceSettingsState.availableVoices,
            draftVoiceId = voiceSettingsState.draftVoiceId,
            draftSpeechRate = voiceSettingsState.draftSpeed,
            onDraftVoiceChange = { onDialogEvent(SettingsDialogEvent.DraftChange.VoiceSettingsVoice(it)) },
            onDraftSpeechRateChange = { onDialogEvent(SettingsDialogEvent.DraftChange.VoiceSettingsSpeechRate(it)) },
            onConfirm = { onDialogEvent(SettingsDialogEvent.Confirm) },
            onDismiss = { onDialogEvent(SettingsDialogEvent.Dismiss) },
        )
    }
}
