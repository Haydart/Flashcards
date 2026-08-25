package com.rossomak.flashcards.feature.settings

import com.rossomak.flashcards.core.ui.dialog.DialogEvent
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState

/** The event type this screen's dialogs report through. See [DialogEvent]. */
typealias SettingsDialogEvent = DialogEvent<SettingsDialog>

/**
 * Which dialog the Settings screen currently has open, and the draft it is editing.
 *
 * This is the screen's whole dialog contract: the set below is what it can show, and each case
 * states what that dialog carries. Nothing restates the set — opening one means handing over an
 * instance from here, so there is no parallel hierarchy to keep in sync.
 *
 * One case today, and still a sealed field rather than a boolean: the screen is a stub that will
 * grow rows for the study defaults, and this is the shape they plug into (ADR-0036).
 */
sealed interface SettingsDialog {

    /**
     * The draft lives here like every other dialog's, but is the one this screen cannot seed at the
     * call site: it comes from
     * [VoiceSettingsController][com.rossomak.flashcards.core.ui.voice.VoiceSettingsController]'s
     * saved settings and voice cache, which the row does not have. The ViewModel always replaces
     * what it is handed, so the default here is a placeholder, never a value in use.
     */
    data class VoiceSettings(val draft: VoiceSettingsDraftState = VoiceSettingsDraftState()) : SettingsDialog
}
