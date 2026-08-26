package com.rossomak.flashcards.feature.settings

import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.VoiceOption

/**
 * Everything the Settings rows render.
 *
 * The study and voice-toggle defaults below are held **in memory only**: no repository backs them
 * yet, so they are seeded from the same constants a session falls back to and reset when the
 * process dies. They live here rather than being read from a half-built store so the whole screen
 * has one story — see the TODO on [SettingsViewModel.onDialogConfirm].
 *
 * Voice playback is the exception and is genuinely persisted, through
 * [VoiceSettingsController][com.rossomak.flashcards.core.ui.voice.VoiceSettingsController]:
 * [speechRate] and [voiceId] mirror what it has saved, and [availableVoices] resolves [voiceId] to
 * the name the row shows.
 */
data class SettingsScreenState(
    val sessionLength: Int = StudySessionConfig.DEFAULT_LENGTH,
    val ratedAttempts: Int = StudySessionConfig.DEFAULT_RATED_ATTEMPTS,
    val defaultStudyMode: StudyMode = StudyMode.Rated,
    val sortOrder: FlashcardSortOrder = FlashcardSortOrder.Default,
    val voiceAnsweringEnabled: Boolean = false,
    val readAloudEnabled: Boolean = false,
    val speechRate: Float = 1f,
    val voiceId: String? = null,
    val availableVoices: List<VoiceOption> = emptyList(),
    val isSigningOut: Boolean = false,
    val activeDialog: SettingsDialog? = null,
) {

    /**
     * The selected voice's display name, or `null` while the platform voice list has not arrived
     * yet or no longer contains the saved id (an engine can be uninstalled between runs). The row
     * falls back to showing the speech rate alone rather than an id no one can read.
     */
    val voiceName: String? get() = availableVoices.firstOrNull { it.id == voiceId }?.displayName
}
