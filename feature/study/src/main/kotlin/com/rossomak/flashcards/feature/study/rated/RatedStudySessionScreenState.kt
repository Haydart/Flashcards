package com.rossomak.flashcards.feature.study.rated

import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerPhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState

/**
 * Everything a Rated Study Session screen renders. No Study Mode field — the type itself is the
 * mode (ticket 03 of
 * [ADR-0045](../../../../../../../../docs/adr/0045-separate-fast-and-rated-session-screens.md)),
 * and no voice-auto-start-pending flag — that is Fast's; Rated never auto-starts playback, only
 * voice answering switches the gateway on.
 *
 * Deliberately duplicates the shape of `FastStudySessionScreenState` rather than sharing a base
 * type with it: this screen is about to grow an attempt counter and a per-card ledger in the next
 * spec in the sequence, and a shared base would need a `when` on mode to stay useful — exactly the
 * branching this split exists to remove.
 */
data class RatedStudySessionScreenState(
    val sessionTitle: String = "",
    val isLoading: Boolean = false,
    val flashcards: List<Flashcard> = emptyList(),
    val currentCardIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val error: String? = null,
    val isVoiceActive: Boolean = false,
    val isVoicePlaying: Boolean = false,
    val speechRate: Float = VoicePlaybackState.DEFAULT_SPEECH_RATE,
    val voiceError: String? = null,
    val curationError: String? = null,
    val isVoiceAnswerEnabled: Boolean = false,
    val voiceAnswerPhase: VoiceAnswerPhase = VoiceAnswerPhase.Idle,
    val voiceAnswerSanitizedTranscript: String? = null,
    val lastVoiceAnswerGrade: VoiceAnswerGrade? = null,
    val voiceAnswerError: String? = null,
    val isMicPermissionRequestPending: Boolean = false,
    val activeDialog: StudySessionDialog? = null,
    // Mirrors RatedSessionState.masteredCount; carried now so ticket 03's counter widget doesn't
    // need to reshape this state to read it.
    val masteredCount: Int = 0,
) {
    val currentCard: Flashcard? get() = flashcards.getOrNull(currentCardIndex)
}
