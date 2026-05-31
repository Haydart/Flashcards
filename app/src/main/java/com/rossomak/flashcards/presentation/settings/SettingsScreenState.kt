package com.rossomak.flashcards.presentation.settings

data class SettingsScreenState(
	val isSigningOut: Boolean = false,
	val navigationDestination: SettingsDestination? = null
)

