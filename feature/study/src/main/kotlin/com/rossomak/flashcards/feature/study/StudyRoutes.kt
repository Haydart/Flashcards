package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import kotlinx.serialization.Serializable

/**
 * @param difficultyMin lower bound of the difficulty filter, flattened out of an `IntRange` because
 * androidx.navigation only derives a NavType for primitives and enums — the same reason
 * [StudySessionRoute] flattens its voice settings.
 * @param difficultyMax upper bound, paired with [difficultyMin].
 * @param sortOrder **null means "nothing upstream chose an order, use my saved default"**. The
 * nullability is load-bearing: a non-null field could not tell a deliberate
 * [FlashcardSortOrder.Default] apart from an absent choice, and the Quick Session path from Category
 * Details genuinely has no list behind it to inherit an order from (ADR-0038).
 */
@Serializable
data class PreviewStudySessionRoute(
    val categoryId: String,
    val categoryName: String,
    val subcategoryIds: List<String>,
    val subcategoryNames: List<String>,
    val filterTagIds: List<String> = emptyList(),
    val difficultyMin: Int = StudySessionConfig.MIN_DIFFICULTY,
    val difficultyMax: Int = StudySessionConfig.MAX_DIFFICULTY,
    val sortOrder: FlashcardSortOrder? = null,
    val isQuickSession: Boolean = false,
) {
    val difficultyRange: IntRange get() = difficultyMin..difficultyMax
}

/**
 * @param voiceAnsweringEnabled the Preview screen's choice (ADR-0030). Honoured on entry for
 * Rated sessions only — Fast mode has no rating step for voice answering to drive (ADR-0025).
 * @param ratedAttempts the Preview screen's confirmed choice, carried through so it reaches the
 * session rather than being silently dropped. Not yet acted on here — the session has no
 * retry-on-fail behavior to drive it yet.
 * @param readAloudEnabled the Preview screen's confirmed choice, carried through for the same
 * reason as [ratedAttempts]. Not yet acted on here — the session has no auto-play behavior to
 * drive it yet.
 * @param speechRate the Preview screen's confirmed [VoiceSettings.speechRate], session-scoped from
 * here on: a mid-session change updates only this running session (unless the user keeps it as
 * default), never a nullable "override" of some other source of truth. Flattened onto the route
 * rather than nesting [VoiceSettings] itself — androidx.navigation's typesafe routes only derive a
 * NavType for primitives and enums, not arbitrary data classes.
 * @param voiceId the Preview screen's confirmed [VoiceSettings.voiceId], flattened for the same
 * reason as [speechRate].
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
    val speechRate: Float = VoiceSettings().speechRate,
    val voiceId: String? = VoiceSettings().voiceId,
) {
    val voiceSettings: VoiceSettings
        get() = VoiceSettings(speechRate = speechRate, voiceId = voiceId)
}

@Serializable
data object StudySummaryRoute
