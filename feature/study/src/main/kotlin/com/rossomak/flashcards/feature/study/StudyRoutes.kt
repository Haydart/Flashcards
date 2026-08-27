package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
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
)

@Serializable
data object StudySummaryRoute
