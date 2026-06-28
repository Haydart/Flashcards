package com.rossomak.flashcards.feature.settings

data class SettingsScreenState(
    val isSigningOut: Boolean = false,
    val navigateToLogin: Boolean = false
)
