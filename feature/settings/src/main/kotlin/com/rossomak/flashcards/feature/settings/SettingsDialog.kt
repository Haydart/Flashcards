package com.rossomak.flashcards.feature.settings

/**
 * Which dialog the Settings screen currently has open.
 *
 * One case today, and still a sealed field rather than a boolean: the screen is a stub that will
 * grow rows for the study defaults, and this is the shape they plug into. It also keeps the
 * visibility of the shared voice-settings dialog owned by the screen rather than by
 * [VoiceSettingsController][com.rossomak.flashcards.core.ui.voice.VoiceSettingsController], which
 * both this screen and the study session inject their own copy of.
 */
sealed interface SettingsDialog {

    data object VoiceSettings : SettingsDialog
}
