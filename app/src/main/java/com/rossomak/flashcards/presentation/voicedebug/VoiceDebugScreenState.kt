package com.rossomak.flashcards.presentation.voicedebug

import com.rossomak.flashcards.core.data.network.VoicePipelineDebugSettings

data class VoiceDebugScreenState(
    val micRouteLabel: String = "",
    val playbackRouteLabel: String = "Idle",
    val isVadListening: Boolean = false,
    val isSpeechDetected: Boolean = false,
    val vadSpeechProbability: Float = 0f,
    val hasCapturedUtterance: Boolean = false,
    val capturedUtteranceDurationMs: Long = 0L,
    val vadEventLog: List<String> = emptyList(),
    val isRecordingClip: Boolean = false,
    val rawClipDurationMs: Long = 0L,
    val hasRawClip: Boolean = false,
    val isTranscribing: Boolean = false,
    val transcriptionResult: String? = null,
    val gradeQuestion: String = "",
    val gradeExpectedAnswer: String = "",
    val gradeTranscript: String = "",
    val isGrading: Boolean = false,
    val gradeResultJson: String? = null,
    val isCheckingEntitlement: Boolean = false,
    val entitlementResult: String? = null,
    val toggles: VoicePipelineDebugSettings.StageToggles = VoicePipelineDebugSettings.StageToggles(),
    val isRealBackendConfigured: Boolean = false
)
