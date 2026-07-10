package com.rossomak.flashcards.presentation.voicedebug

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.data.network.VoiceGradingApiRouter
import com.rossomak.flashcards.core.data.network.VoicePipelineDebugSettings
import com.rossomak.flashcards.core.domain.usecase.CheckVoiceGradingEntitlementUseCase
import com.rossomak.flashcards.core.domain.usecase.SanitizeAndGradeTranscriptUseCase
import com.rossomak.flashcards.core.domain.usecase.TranscribeVoiceClipUseCase
import com.rossomak.flashcards.core.voice.AudioRouteManager
import com.rossomak.flashcards.core.voice.CaptureRouteType
import com.rossomak.flashcards.core.voice.PcmPlayer
import com.rossomak.flashcards.core.voice.SileroVoiceActivityDetector
import com.rossomak.flashcards.core.voice.VoiceCaptureEngine
import com.rossomak.flashcards.core.voice.VoiceCaptureEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Debug-only harness exposing every voice pipeline stage as an independently testable block
 * (design doc §Debug dev screen). Raw request/response data is surfaced verbatim so
 * intermediate output can be inspected while stages are still being built out.
 */
@HiltViewModel
class VoiceDebugViewModel @Inject constructor(
    private val transcribeVoiceClip: TranscribeVoiceClipUseCase,
    private val sanitizeAndGradeTranscript: SanitizeAndGradeTranscriptUseCase,
    private val checkVoiceGradingEntitlement: CheckVoiceGradingEntitlementUseCase,
    private val voiceCaptureEngine: VoiceCaptureEngine,
    private val audioRouteManager: AudioRouteManager,
    private val voiceActivityDetector: SileroVoiceActivityDetector,
    private val pcmPlayer: PcmPlayer,
    private val debugSettings: VoicePipelineDebugSettings,
    voiceGradingApiRouter: VoiceGradingApiRouter,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VoiceDebugScreenState(isRealBackendConfigured = voiceGradingApiRouter.isRealBackendConfigured)
    )
    val state: StateFlow<VoiceDebugScreenState> = _state.asStateFlow()

    private var rawClip: ShortArray = ShortArray(0)
    private var obfuscatedClip: ShortArray = ShortArray(0)
    private var capturedUtterance: ShortArray = ShortArray(0)

    private var lastLoggedMicRouteLabel: String? = null
    private var lastLoggedPlaybackRouteLabel: String? = null

    init {
        // Capture (startListening/recordRawClip) reads AudioRouteManager.route, which defaults to
        // NONE (not capturable) until a session route is acquired — without this the debug screen's
        // capture loop fails immediately with "Bluetooth microphone unavailable", BT state aside.
        viewModelScope.launch { audioRouteManager.acquireSessionRoute() }
        viewModelScope.launch {
            voiceCaptureEngine.isSpeechDetected.collect { isSpeech ->
                _state.update { it.copy(isSpeechDetected = isSpeech) }
            }
        }
        // Mic route indicator: the confirmed device wins while actively recording; falls back to
        // the resolved target route (AudioRouteManager) while idle. Transitions get logged so
        // intermittent BT drops mid-session leave a trail instead of just flashing past.
        viewModelScope.launch {
            combine(
                audioRouteManager.route,
                voiceCaptureEngine.actualMicDevice,
            ) { target, actual -> actual?.toRouteLabel() ?: target.type.toRouteLabel() }
                .collect { label ->
                    _state.update { it.copy(micRouteLabel = label) }
                    logRouteChange("mic", ::lastLoggedMicRouteLabel, label)
                }
        }
        // Playback route indicator: PcmPlayer.actualDevice is null whenever nothing is playing.
        viewModelScope.launch {
            pcmPlayer.actualDevice.collect { device ->
                val label = device?.toRouteLabel() ?: IDLE_ROUTE_LABEL
                _state.update { it.copy(playbackRouteLabel = label) }
                logRouteChange("playback", ::lastLoggedPlaybackRouteLabel, label)
            }
        }
        viewModelScope.launch {
            voiceActivityDetector.speechProbability.collect { probability ->
                _state.update { it.copy(vadSpeechProbability = probability) }
            }
        }
        viewModelScope.launch {
            voiceCaptureEngine.events.collect { event ->
                if (event is VoiceCaptureEvent.UtteranceCaptured) {
                    capturedUtterance = event.utterance.obfuscatedPcm
                    _state.update {
                        it.copy(
                            hasCapturedUtterance = true,
                            capturedUtteranceDurationMs = event.utterance.durationMs,
                        )
                    }
                }
                logVadEvent(event)
            }
        }
        viewModelScope.launch {
            debugSettings.toggles.collect { toggles ->
                _state.update { it.copy(toggles = toggles) }
            }
        }
    }

    // Mic permission is checked by the screen before invoking any capture action.
    @SuppressLint("MissingPermission")
    fun onVadToggle() {
        if (_state.value.isVadListening) {
            voiceCaptureEngine.stopListening()
            _state.update { it.copy(isVadListening = false) }
        } else {
            voiceCaptureEngine.startListening()
            _state.update { it.copy(isVadListening = true) }
        }
    }

    @SuppressLint("MissingPermission")
    fun onRecordClip() {
        if (_state.value.isRecordingClip) return
        _state.update { it.copy(isRecordingClip = true) }
        viewModelScope.launch {
            try {
                rawClip = voiceCaptureEngine.recordRawClip(RAW_CLIP_DURATION_MS)
                obfuscatedClip = ShortArray(0)
                _state.update {
                    it.copy(
                        hasRawClip = rawClip.isNotEmpty(),
                        rawClipDurationMs = rawClip.size * 1000L / VoiceCaptureEngine.SAMPLE_RATE_HZ,
                        transcriptionResult = null,
                    )
                }
            } finally {
                _state.update { it.copy(isRecordingClip = false) }
            }
        }
    }

    fun onPlayRawClip() {
        if (rawClip.isNotEmpty()) pcmPlayer.play(rawClip)
    }

    /** Plays back the last VAD-bounded utterance (obfuscated PCM) to audit the VAD boundaries. */
    fun onPlayCapturedUtterance() {
        if (capturedUtterance.isNotEmpty()) pcmPlayer.play(capturedUtterance)
    }

    fun onPlayObfuscatedClip() {
        if (rawClip.isEmpty()) return
        if (obfuscatedClip.isEmpty()) obfuscatedClip = voiceCaptureEngine.obfuscate(rawClip)
        pcmPlayer.play(obfuscatedClip)
    }

    fun onRerandomizeObfuscation() {
        voiceCaptureEngine.rerandomizeObfuscation()
        obfuscatedClip = ShortArray(0)
    }

    fun onTranscribeClip() {
        if (rawClip.isEmpty() || _state.value.isTranscribing) return
        _state.update { it.copy(isTranscribing = true) }
        viewModelScope.launch {
            try {
                if (obfuscatedClip.isEmpty()) obfuscatedClip = voiceCaptureEngine.obfuscate(rawClip)
                val wavBytes = voiceCaptureEngine.encodeWav(obfuscatedClip)
                transcribeVoiceClip(wavBytes)
                    .onSuccess { transcript ->
                        _state.update { it.copy(transcriptionResult = transcript) }
                    }
                    .onFailure { error ->
                        _state.update { it.copy(transcriptionResult = "ERROR: ${error.message}") }
                    }
            } finally {
                _state.update { it.copy(isTranscribing = false) }
            }
        }
    }

    fun onGradeQuestionChange(value: String) = _state.update { it.copy(gradeQuestion = value) }

    fun onGradeExpectedAnswerChange(value: String) =
        _state.update { it.copy(gradeExpectedAnswer = value) }

    fun onGradeTranscriptChange(value: String) = _state.update { it.copy(gradeTranscript = value) }

    fun onSanitizeAndGrade() {
        if (_state.value.isGrading) return
        _state.update { it.copy(isGrading = true) }
        viewModelScope.launch {
            try {
                with(_state.value) {
                    sanitizeAndGradeTranscript(
                        SanitizeAndGradeTranscriptUseCase.Params(
                            question = gradeQuestion,
                            expectedAnswer = gradeExpectedAnswer,
                            rawTranscript = gradeTranscript,
                        )
                    )
                }.onSuccess { grade ->
                    val resultJson = JSONObject()
                        .put("sanitized_transcript", grade.sanitizedTranscript)
                        .put("grade", grade.gradePercent)
                        .put("feedback", grade.feedback)
                        .toString(JSON_INDENT_SPACES)
                    _state.update { it.copy(gradeResultJson = resultJson) }
                }.onFailure { error ->
                    _state.update { it.copy(gradeResultJson = "ERROR: ${error.message}") }
                }
            } finally {
                _state.update { it.copy(isGrading = false) }
            }
        }
    }

    fun onCheckEntitlement() {
        if (_state.value.isCheckingEntitlement) return
        _state.update { it.copy(isCheckingEntitlement = true) }
        viewModelScope.launch {
            try {
                checkVoiceGradingEntitlement()
                    .onSuccess { isPremium ->
                        _state.update {
                            it.copy(entitlementResult = JSONObject().put("is_premium", isPremium).toString())
                        }
                    }
                    .onFailure { error ->
                        _state.update { it.copy(entitlementResult = "ERROR: ${error.message}") }
                    }
            } finally {
                _state.update { it.copy(isCheckingEntitlement = false) }
            }
        }
    }

    fun onSimulatePremiumToggle(isPremium: Boolean) =
        debugSettings.setSimulatePremiumEntitlement(isPremium)

    fun onUseRealTranscriptionToggle(useReal: Boolean) =
        debugSettings.setUseRealTranscription(useReal)

    fun onUseRealGradingToggle(useReal: Boolean) = debugSettings.setUseRealGrading(useReal)

    fun onUseRealEntitlementToggle(useReal: Boolean) = debugSettings.setUseRealEntitlement(useReal)

    private fun logVadEvent(event: VoiceCaptureEvent) {
        val label = when (event) {
            is VoiceCaptureEvent.SpeechStarted -> "speech start"
            is VoiceCaptureEvent.SpeechEnded -> "speech end"
            is VoiceCaptureEvent.UtteranceCaptured ->
                "utterance captured (${event.utterance.durationMs} ms)"
            is VoiceCaptureEvent.CaptureFailed -> "capture failed: ${event.reason}"
        }
        appendLog(label)
    }

    /** Appends a route-label transition to the event log, skipping the initial emission. */
    private fun logRouteChange(kind: String, lastLabel: kotlin.reflect.KMutableProperty0<String?>, newLabel: String) {
        val previous = lastLabel.get()
        if (previous != null && previous != newLabel) appendLog("$kind route -> $newLabel")
        lastLabel.set(newLabel)
    }

    private fun appendLog(line: String) {
        val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        _state.update {
            it.copy(vadEventLog = (listOf("$timestamp  $line") + it.vadEventLog).take(MAX_LOG_LINES))
        }
    }

    override fun onCleared() {
        voiceCaptureEngine.stopListening()
        pcmPlayer.stop()
        audioRouteManager.releaseSessionRoute()
        super.onCleared()
    }

    private fun CaptureRouteType.toRouteLabel(): String = when (this) {
        CaptureRouteType.PHONE -> "Phone mic"
        CaptureRouteType.BLUETOOTH_LE -> "Bluetooth (LE Audio)"
        CaptureRouteType.BLUETOOTH_SCO -> "Bluetooth (SCO)"
        CaptureRouteType.WAITING -> "Waiting for Bluetooth link"
        CaptureRouteType.NONE -> "No capturable mic"
    }

    private fun AudioDeviceInfo.toRouteLabel(): String {
        val typeLabel = when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth (SCO)"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth (LE Audio)"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth (A2DP)"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headset"
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB"
            else -> "Unknown ($type)"
        }
        return productName?.toString()?.takeIf { it.isNotBlank() }?.let { "$typeLabel — $it" } ?: typeLabel
    }

    private companion object {
        const val RAW_CLIP_DURATION_MS = 3_000L
        const val MAX_LOG_LINES = 12
        const val JSON_INDENT_SPACES = 2
        const val IDLE_ROUTE_LABEL = "Idle"
    }
}
