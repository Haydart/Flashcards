package com.rossomak.flashcards.feature.settings

data class SettingsScreenState(
    val isSigningOut: Boolean = false,
    val activeDialog: SettingsDialog? = null,
)
