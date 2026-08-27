package com.rossomak.flashcards.feature.settings

import com.rossomak.flashcards.core.domain.model.DailyGoal
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.VoiceOption

/**
 * Everything the Settings rows render.
 *
 * [dailyGoalMinutes] and the study-session rows below it are backed by
 * `UserPreferencesRepository`/`StudySessionPreferencesRepository`: the ViewModel collects both
 * continuously and folds every emission straight in, so a row can never disagree with disk. The
 * non-null constant defaults here are only the pre-emission placeholder — DataStore already has
 * the real value in memory by the time this screen is reached (`SplashViewModel` reads the same
 * store on every cold start).
 *
 * Voice playback is the one row still on its own store, through
 * [VoiceSettingsController][com.rossomak.flashcards.core.ui.voice.VoiceSettingsController]:
 * [speechRate] and [voiceId] mirror what it has saved, and [availableVoices] resolves [voiceId] to
 * the name the row shows.
 */
data class SettingsScreenState(
    val dailyGoalMinutes: Int = DailyGoal.DEFAULT_MINUTES,
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
