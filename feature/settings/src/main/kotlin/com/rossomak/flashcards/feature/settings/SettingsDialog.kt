package com.rossomak.flashcards.feature.settings

import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
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
 * No case carries `keepAsDefault`: on this screen every value *is* the default, so the shared
 * dialogs are all called with `keepAsDefault = null` and the checkbox never renders (ADR-0036).
 */
sealed interface SettingsDialog {

    data class Length(val draft: Int) : SettingsDialog

    data class Goal(val draft: Int) : SettingsDialog

    data class Attempts(val draft: Int) : SettingsDialog

    data class Mode(val draft: StudyMode) : SettingsDialog

    data class Sort(val draft: FlashcardSortOrder) : SettingsDialog

    data class VoiceAnswering(val draft: Boolean) : SettingsDialog

    data class ReadAloud(val draft: Boolean) : SettingsDialog

    /**
     * The draft lives here like every other dialog's, but is the one this screen cannot seed at the
     * call site: it comes from
     * [VoiceSettingsController][com.rossomak.flashcards.core.ui.voice.VoiceSettingsController]'s
     * saved settings and voice cache, which the row does not have. The ViewModel always replaces
     * what it is handed, so the default here is a placeholder, never a value in use.
     */
    data class VoiceSettings(val draft: VoiceSettingsDraftState = VoiceSettingsDraftState()) : SettingsDialog

    /**
     * The one dialog with no draft: confirming it commits nothing, it runs sign-out and lets the
     * ViewModel answer with a navigation event — the same shape as "Exit session?" (ADR-0036).
     */
    data object SignOut : SettingsDialog
}
