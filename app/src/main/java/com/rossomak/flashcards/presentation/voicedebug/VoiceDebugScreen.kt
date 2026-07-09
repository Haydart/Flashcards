package com.rossomak.flashcards.presentation.voicedebug

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.R

@Composable
fun VoiceDebugScreen(modifier: Modifier = Modifier, viewModel: VoiceDebugViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingMicAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micPermissionDeniedMessage = stringResource(R.string.voice_debug_mic_permission_message)
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingMicAction?.invoke()
        } else {
            Toast.makeText(context, micPermissionDeniedMessage, Toast.LENGTH_SHORT).show()
        }
        pendingMicAction = null
    }

    fun withMicPermission(action: () -> Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            action()
        } else {
            pendingMicAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    VoiceDebugContent(
        modifier = modifier,
        state = state,
        onVadToggle = { withMicPermission(viewModel::onVadToggle) },
        onPlayCapturedUtterance = viewModel::onPlayCapturedUtterance,
        onRecordClip = { withMicPermission(viewModel::onRecordClip) },
        onPlayRawClip = viewModel::onPlayRawClip,
        onPlayObfuscatedClip = viewModel::onPlayObfuscatedClip,
        onRerandomizeObfuscation = viewModel::onRerandomizeObfuscation,
        onTranscribeClip = viewModel::onTranscribeClip,
        onGradeQuestionChange = viewModel::onGradeQuestionChange,
        onGradeExpectedAnswerChange = viewModel::onGradeExpectedAnswerChange,
        onGradeTranscriptChange = viewModel::onGradeTranscriptChange,
        onSanitizeAndGrade = viewModel::onSanitizeAndGrade,
        onCheckEntitlement = viewModel::onCheckEntitlement,
        onSimulatePremiumToggle = viewModel::onSimulatePremiumToggle,
        onUseRealTranscriptionToggle = viewModel::onUseRealTranscriptionToggle,
        onUseRealGradingToggle = viewModel::onUseRealGradingToggle,
        onUseRealEntitlementToggle = viewModel::onUseRealEntitlementToggle
    )
}

@Composable
fun VoiceDebugContent(
    modifier: Modifier = Modifier,
    state: VoiceDebugScreenState,
    onVadToggle: () -> Unit,
    onPlayCapturedUtterance: () -> Unit,
    onRecordClip: () -> Unit,
    onPlayRawClip: () -> Unit,
    onPlayObfuscatedClip: () -> Unit,
    onRerandomizeObfuscation: () -> Unit,
    onTranscribeClip: () -> Unit,
    onGradeQuestionChange: (String) -> Unit,
    onGradeExpectedAnswerChange: (String) -> Unit,
    onGradeTranscriptChange: (String) -> Unit,
    onSanitizeAndGrade: () -> Unit,
    onCheckEntitlement: () -> Unit,
    onSimulatePremiumToggle: (Boolean) -> Unit,
    onUseRealTranscriptionToggle: (Boolean) -> Unit,
    onUseRealGradingToggle: (Boolean) -> Unit,
    onUseRealEntitlementToggle: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.voice_debug_title),
            style = MaterialTheme.typography.titleLarge
        )

        DebugBlock(title = stringResource(R.string.voice_debug_vad_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onVadToggle) {
                    Text(
                        stringResource(
                            if (state.isVadListening) {
                                R.string.voice_debug_vad_stop_button
                            } else {
                                R.string.voice_debug_vad_start_button
                            }
                        )
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    color = if (state.isSpeechDetected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant
                ) {}
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        if (state.isSpeechDetected) {
                            R.string.voice_debug_vad_speech_label
                        } else {
                            R.string.voice_debug_vad_silence_label
                        }
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = stringResource(R.string.voice_debug_vad_probability_label, state.vadSpeechProbability),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPlayCapturedUtterance, enabled = state.hasCapturedUtterance) {
                    Text(stringResource(R.string.voice_debug_vad_play_utterance_button))
                }
                if (state.hasCapturedUtterance) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(
                            R.string.voice_debug_vad_utterance_ready_label,
                            state.capturedUtteranceDurationMs
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            state.vadEventLog.forEach { line -> RawDataText(line) }
        }

        DebugBlock(title = stringResource(R.string.voice_debug_capture_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRecordClip, enabled = !state.isRecordingClip) {
                    Text(
                        stringResource(
                            if (state.isRecordingClip) {
                                R.string.voice_debug_capture_recording_label
                            } else {
                                R.string.voice_debug_capture_record_button
                            }
                        )
                    )
                }
                OutlinedButton(onClick = onPlayRawClip, enabled = state.hasRawClip) {
                    Text(stringResource(R.string.voice_debug_capture_play_raw_button))
                }
            }
            if (state.hasRawClip) {
                Text(
                    text = stringResource(R.string.voice_debug_capture_clip_ready_label, state.rawClipDurationMs),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        DebugBlock(title = stringResource(R.string.voice_debug_obfuscation_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPlayRawClip, enabled = state.hasRawClip) {
                    Text(stringResource(R.string.voice_debug_capture_play_raw_button))
                }
                Button(onClick = onPlayObfuscatedClip, enabled = state.hasRawClip) {
                    Text(stringResource(R.string.voice_debug_obfuscation_play_obfuscated_button))
                }
            }
            OutlinedButton(onClick = onRerandomizeObfuscation) {
                Text(stringResource(R.string.voice_debug_obfuscation_rerandomize_button))
            }
            if (!state.hasRawClip) {
                Text(
                    text = stringResource(R.string.voice_debug_obfuscation_no_clip_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        DebugBlock(title = stringResource(R.string.voice_debug_transcription_title)) {
            Button(onClick = onTranscribeClip, enabled = state.hasRawClip && !state.isTranscribing) {
                Text(
                    stringResource(
                        if (state.isTranscribing) {
                            R.string.voice_debug_transcription_running_label
                        } else {
                            R.string.voice_debug_transcription_send_button
                        }
                    )
                )
            }
            state.transcriptionResult?.let { RawDataText(it) }
        }

        DebugBlock(title = stringResource(R.string.voice_debug_grade_title)) {
            OutlinedTextField(
                value = state.gradeQuestion,
                onValueChange = onGradeQuestionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.voice_debug_grade_question_label)) }
            )
            OutlinedTextField(
                value = state.gradeExpectedAnswer,
                onValueChange = onGradeExpectedAnswerChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.voice_debug_grade_expected_answer_label)) }
            )
            OutlinedTextField(
                value = state.gradeTranscript,
                onValueChange = onGradeTranscriptChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.voice_debug_grade_transcript_label)) }
            )
            Button(onClick = onSanitizeAndGrade, enabled = !state.isGrading) {
                Text(
                    stringResource(
                        if (state.isGrading) {
                            R.string.voice_debug_grade_running_label
                        } else {
                            R.string.voice_debug_grade_send_button
                        }
                    )
                )
            }
            state.gradeResultJson?.let { RawDataText(it) }
        }

        DebugBlock(title = stringResource(R.string.voice_debug_entitlement_title)) {
            LabeledSwitch(
                label = stringResource(R.string.voice_debug_entitlement_simulate_premium_label),
                checked = state.toggles.simulatePremiumEntitlement,
                onCheckedChange = onSimulatePremiumToggle
            )
            Button(onClick = onCheckEntitlement, enabled = !state.isCheckingEntitlement) {
                Text(
                    stringResource(
                        if (state.isCheckingEntitlement) {
                            R.string.voice_debug_entitlement_checking_label
                        } else {
                            R.string.voice_debug_entitlement_check_button
                        }
                    )
                )
            }
            state.entitlementResult?.let { RawDataText(it) }
        }

        DebugBlock(title = stringResource(R.string.voice_debug_toggles_title)) {
            if (!state.isRealBackendConfigured) {
                Text(
                    text = stringResource(R.string.voice_debug_toggles_backend_unconfigured_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            LabeledSwitch(
                label = stringResource(R.string.voice_debug_toggle_transcription_label),
                checked = state.toggles.useRealTranscription,
                enabled = state.isRealBackendConfigured,
                onCheckedChange = onUseRealTranscriptionToggle
            )
            LabeledSwitch(
                label = stringResource(R.string.voice_debug_toggle_grading_label),
                checked = state.toggles.useRealGrading,
                enabled = state.isRealBackendConfigured,
                onCheckedChange = onUseRealGradingToggle
            )
            LabeledSwitch(
                label = stringResource(R.string.voice_debug_toggle_entitlement_label),
                checked = state.toggles.useRealEntitlement,
                enabled = state.isRealBackendConfigured,
                onCheckedChange = onUseRealEntitlementToggle
            )
        }
    }
}

@Composable
private fun DebugBlock(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun RawDataText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
            .horizontalScroll(rememberScrollState()),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
    Spacer(modifier = Modifier.height(2.dp))
}
