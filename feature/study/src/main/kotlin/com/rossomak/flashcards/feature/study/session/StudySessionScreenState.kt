package com.rossomak.flashcards.feature.study.session

import com.rossomak.flashcards.core.domain.model.CurationRequest
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerPhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState

data class StudySessionScreenState(
    val sessionTitle: String = "",
    val studyMode: StudyMode = StudyMode.RATED,
    val isLoading: Boolean = false,
    val flashcards: List<Flashcard> = emptyList(),
    val currentCardIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val isSessionComplete: Boolean = false,
    val error: String? = null,
    val isVoiceActive: Boolean = false,
    val isVoicePlaying: Boolean = false,
    val isVoiceAutoStartPending: Boolean = false,
    val speechRate: Float = VoicePlaybackState.DEFAULT_SPEECH_RATE,
    val voiceError: String? = null,
    val isCurationDialogVisible: Boolean = false,
    val curationRequests: Map<String, CurationRequest> = emptyMap(),
    val curationError: String? = null,
    val voiceSettingsState: VoiceSettingsDraftState = VoiceSettingsDraftState(),
    val isVoiceAnswerEnabled: Boolean = false,
    val voiceAnswerPhase: VoiceAnswerPhase = VoiceAnswerPhase.IDLE,
    val lastVoiceAnswerGrade: VoiceAnswerGrade? = null,
    val voiceAnswerError: String? = null,
    val isVoiceAnswerConsentDialogVisible: Boolean = false,
    val isMicPermissionRequestPending: Boolean = false
)
