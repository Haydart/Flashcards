package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import kotlinx.serialization.Serializable

@Serializable
data class PreviewStudySessionRoute(
    val categoryId: String,
    val categoryName: String,
    val subcategoryIds: List<String>,
    val subcategoryNames: List<String>,
    val filterTagIds: List<String> = emptyList(),
    val isQuickSession: Boolean = false,
)

/**
 * @param voiceAnsweringEnabled the Preview screen's choice (ADR-0030). Honoured on entry for
 * Rated sessions only — Fast mode has no rating step for voice answering to drive (ADR-0025).
 * @param ratedAttempts the Preview screen's confirmed choice, carried through so it reaches the
 * session rather than being silently dropped. Not yet acted on here — the session has no
 * retry-on-fail behavior to drive it yet.
 * @param readAloudEnabled the Preview screen's confirmed choice, carried through for the same
 * reason as [ratedAttempts]. Not yet acted on here — the session has no auto-play behavior to
 * drive it yet.
 * @param voiceSettings the Preview screen's confirmed choice, session-scoped from here on: a
 * mid-session change updates only this running session (unless the user keeps it as default),
 * never a nullable "override" of some other source of truth.
 */
@Serializable
data class StudySessionRoute(
    val categoryId: String,
    val sessionTitle: String,
    val subcategoryIds: List<String>,
    val cardIds: List<String>,
    val studyMode: StudyMode,
    val voiceAnsweringEnabled: Boolean = false,
    val ratedAttempts: Int = StudySessionConfig.DEFAULT_RATED_ATTEMPTS,
    val readAloudEnabled: Boolean = false,
    val voiceSettings: VoiceSettings = VoiceSettings(),
)

@Serializable
data object StudySummaryRoute
