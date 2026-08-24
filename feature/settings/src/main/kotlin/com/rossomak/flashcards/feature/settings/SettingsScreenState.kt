package com.rossomak.flashcards.feature.settings

import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState

data class SettingsScreenState(
    val isSigningOut: Boolean = false,
    val voiceSettingsState: VoiceSettingsDraftState = VoiceSettingsDraftState(),
    val activeDialog: SettingsDialog? = null,
)
