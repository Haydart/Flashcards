package com.rossomak.flashcards.feature.study

import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import kotlinx.serialization.Serializable

/**
 * @param difficultyMin lower bound of the difficulty filter, flattened out of an `IntRange` because
 * androidx.navigation only derives a NavType for primitives and enums — the same reason
 * [FastStudySessionRoute] and [RatedStudySessionRoute] flatten their voice settings.
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
 * Everything a Fast Study Session consumes, and nothing else (ticket 02 of
 * [ADR-0045](../../../docs/adr/0045-separate-fast-and-rated-session-screens.md)). Rated concepts —
 * attempts, voice answering — do not appear; Fast has no path to either.
 *
 * @param readAloudEnabled the Preview screen's confirmed choice. Auto-start is conditional on this
 * flag as well as on having cards, so a session with it off never requests notification permission
 * and never starts text-to-speech.
 * @param speechRate the Preview screen's confirmed `VoiceSettings.speechRate`, flattened onto the
 * route for the same reason as [RatedStudySessionRoute.speechRate] — androidx.navigation's typesafe
 * routes only derive a NavType for primitives and enums.
 * @param voiceId the Preview screen's confirmed `VoiceSettings.voiceId`, flattened for the same
 * reason as [speechRate].
 */
@Serializable
data class FastStudySessionRoute(
    val categoryId: String,
    val sessionTitle: String,
    val subcategoryIds: List<String>,
    val cardIds: List<String>,
    val readAloudEnabled: Boolean = false,
    val speechRate: Float = VoiceSettings().speechRate,
    val voiceId: String? = VoiceSettings().voiceId,
) {
    val voiceSettings: VoiceSettings
        get() = VoiceSettings(speechRate = speechRate, voiceId = voiceId)
}

/**
 * Everything a Rated Study Session consumes, and nothing else (ticket 03 of
 * [ADR-0045](../../../docs/adr/0045-separate-fast-and-rated-session-screens.md)). Read-aloud does
 * not appear — it is a Fast concept.
 *
 * @param voiceAnsweringEnabled the Preview screen's choice (ADR-0030). Honoured on entry, reading
 * consent as a one-shot rather than from the observed flag — the collector may not have emitted by
 * the time the cards land.
 * @param ratedAttempts the Preview screen's confirmed choice, carried through so it reaches the
 * session rather than being silently dropped. Not yet acted on here — the session has no
 * retry-on-fail behavior to drive it until the next spec in the sequence.
 * @param speechRate the Preview screen's confirmed `VoiceSettings.speechRate`, session-scoped from
 * here on: a mid-session change updates only this running session (unless the user keeps it as
 * default). Flattened onto the route for the same reason as [FastStudySessionRoute.speechRate] —
 * androidx.navigation's typesafe routes only derive a NavType for primitives and enums.
 * @param voiceId the Preview screen's confirmed `VoiceSettings.voiceId`, flattened for the same
 * reason as [speechRate].
 */
@Serializable
data class RatedStudySessionRoute(
    val categoryId: String,
    val sessionTitle: String,
    val subcategoryIds: List<String>,
    val cardIds: List<String>,
    val voiceAnsweringEnabled: Boolean = false,
    val ratedAttempts: Int = StudySessionConfig.DEFAULT_RATED_ATTEMPTS,
    val speechRate: Float = VoiceSettings().speechRate,
    val voiceId: String? = VoiceSettings().voiceId,
) {
    val voiceSettings: VoiceSettings
        get() = VoiceSettings(speechRate = speechRate, voiceId = voiceId)
}

/**
 * @deprecated retained until ticket 04 removes the combined session screen that consumes it.
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
