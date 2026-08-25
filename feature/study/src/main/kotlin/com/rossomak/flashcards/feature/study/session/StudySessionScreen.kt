package com.rossomak.flashcards.feature.study.session

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallatinapps.syntaxmp.tokenizer.SyntaxTokenizer
import com.rossomak.flashcards.core.domain.model.FlashcardRating
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.composables.SyntaxCodeBlock
import com.rossomak.flashcards.core.ui.composables.withInlineCode
import com.rossomak.flashcards.core.ui.theme.semanticColors
import com.rossomak.flashcards.feature.study.R
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerPhase
import kotlinx.coroutines.launch

@Composable
fun StudySessionScreen(
    modifier: Modifier = Modifier,
    viewModel: StudySessionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) viewModel.onVoiceAutoStart() else viewModel.onVoiceAutoStartDeclined()
    }

    LaunchedEffect(state.isVoiceAutoStartPending) {
        if (!state.isVoiceAutoStartPending) return@LaunchedEffect
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onVoiceAutoStart()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted -> viewModel.onMicPermissionResult(isGranted) }

    LaunchedEffect(state.isMicPermissionRequestPending) {
        if (!state.isMicPermissionRequestPending) return@LaunchedEffect
        val isAlreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (isAlreadyGranted) {
            viewModel.onMicPermissionResult(true)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(state.lastVoiceAnswerGrade) {
        val grade = state.lastVoiceAnswerGrade ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.study_session_voice_answer_grade_message,
                grade.gradePercent,
                grade.feedback,
            ),
            duration = SnackbarDuration.Short,
        )
        viewModel.onVoiceAnswerGradeDismissed()
    }

    LaunchedEffect(state.voiceAnswerError) {
        if (state.voiceAnswerError == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.study_session_voice_answer_error_message),
            duration = SnackbarDuration.Short,
        )
    }

    LaunchedEffect(state.isSessionComplete) {
        if (state.isSessionComplete) onNavigateBack()
    }

    LaunchedEffect(state.voiceError) {
        val error = state.voiceError ?: return@LaunchedEffect
        launch {
            val result = snackbarHostState.showSnackbar(
                message = "Voice playback unavailable on this device",
                actionLabel = "Open Settings",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                context.startActivity(
                    Intent("com.android.settings.TTS_SETTINGS").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            viewModel.onVoiceErrorDismissed()
        }
    }

    LaunchedEffect(state.curationError) {
        val error = state.curationError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
        viewModel.onCurationErrorDismissed()
    }

    StudySessionContent(
        modifier = modifier,
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onShowAnswer = viewModel::onShowAnswer,
        onNextCard = viewModel::onNextCard,
        onRating = viewModel::onRating,
        onVoicePlayPause = viewModel::onVoicePlayPause,
        onVoiceNext = viewModel::onVoiceNext,
        onVoicePrevious = viewModel::onVoicePrevious,
        onVoiceAnswerToggle = viewModel::onVoiceAnswerToggle,
        onDialogEvent = viewModel::onDialogEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionContent(
    modifier: Modifier = Modifier,
    state: StudySessionScreenState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onShowAnswer: () -> Unit,
    onNextCard: () -> Unit,
    onRating: (FlashcardRating) -> Unit,
    onVoicePlayPause: () -> Unit,
    onVoiceNext: () -> Unit,
    onVoicePrevious: () -> Unit,
    onVoiceAnswerToggle: () -> Unit,
    onDialogEvent: (StudySessionDialogEvent) -> Unit,
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = true),
    )

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        sheetSwipeEnabled = false,
        sheetPeekHeight = when {
            state.isVoiceActive -> 176.dp
            state.studyMode == StudyMode.Rated && state.isAnswerRevealed -> 200.dp
            state.studyMode == StudyMode.Rated -> 152.dp
            state.isAnswerRevealed -> 160.dp
            else -> 112.dp
        },
        sheetDragHandle = {},
        topBar = {
            TopAppBar(
                title = { Text(text = state.sessionTitle) },
                navigationIcon = {
                    IconButton(onClick = { onDialogEvent(StudySessionDialogEvent.Open.ExitSession) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.exit_session_dialog_title),
                        )
                    }
                },
                actions = {
                    if (state.currentCard != null) {
                        IconButton(onClick = { onDialogEvent(StudySessionDialogEvent.Open.ReportProblem) }) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = stringResource(R.string.report_problem_dialog_title),
                            )
                        }
                    }
                    if (state.flashcards.isNotEmpty()) {
                        Text(
                            text = "${state.currentCardIndex + 1} / ${state.flashcards.size}",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            )
        },
        sheetContent = {
            StudySessionSheetContent(
                state = state,
                onShowAnswer = onShowAnswer,
                onRating = onRating,
                onVoicePlayPause = onVoicePlayPause,
                onVoiceNext = onVoiceNext,
                onVoicePrevious = onVoicePrevious,
                onVoiceSettingsCogClick = { onDialogEvent(StudySessionDialogEvent.Open.VoiceSettings) },
                onVoiceAnswerToggle = onVoiceAnswerToggle,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CenteredBox(innerPadding) { CircularProgressIndicator() }
                state.error != null -> CenteredBox(innerPadding) { Text(text = state.error) }
                state.flashcards.isEmpty() -> CenteredBox(innerPadding) {
                    Text(text = stringResource(R.string.study_session_no_cards_message))
                }

                else -> {
                    val card = state.flashcards[state.currentCardIndex]
                    val syntaxEngine = remember { SyntaxTokenizer() }

                    @OptIn(ExperimentalLayoutApi::class)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (card.tags.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                card.tags.forEach { tag ->
                                    androidx.compose.material3.Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = tag,
                                            modifier = Modifier.padding(6.dp, 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Question",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "Difficulty ${card.difficulty}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card.question.withInlineCode(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        card.questionCode?.forEach { codeBlock ->
                            Spacer(modifier = Modifier.height(8.dp))
                            SyntaxCodeBlock(
                                code = codeBlock.code,
                                language = codeBlock.language,
                                engine = syntaxEngine
                            )
                        }
                        AnimatedVisibility(
                            visible = state.isAnswerRevealed,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Answer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = card.answer.withInlineCode(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                card.answerCode?.forEach { codeBlock ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SyntaxCodeBlock(
                                        code = codeBlock.code,
                                        language = codeBlock.language,
                                        engine = syntaxEngine
                                    )
                                }
                                if (!card.extendedContext.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SuggestionChip(
                                        onClick = { onDialogEvent(StudySessionDialogEvent.Open.ExtendedContext) },
                                        label = { Text("Extended context") },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } // end Box

        StudySessionDialogHost(
            activeDialog = state.activeDialog,
            currentCard = state.currentCard,
            voiceSettingsState = state.voiceSettingsState,
            onDialogEvent = onDialogEvent,
            onExitSession = onNavigateBack,
        )
    }
}

@Composable
private fun StudySessionSheetContent(
    state: StudySessionScreenState,
    onShowAnswer: () -> Unit,
    onRating: (FlashcardRating) -> Unit,
    onVoicePlayPause: () -> Unit,
    onVoiceNext: () -> Unit,
    onVoicePrevious: () -> Unit,
    onVoiceSettingsCogClick: () -> Unit,
    onVoiceAnswerToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        if (state.isVoiceActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.studyMode == StudyMode.Rated && state.isVoiceAnswerEnabled) {
                    Text(
                        text = stringResource(
                            when (state.voiceAnswerPhase) {
                                VoiceAnswerPhase.WaitingForQuestion -> R.string.study_session_voice_answer_waiting_label
                                VoiceAnswerPhase.Grading -> R.string.study_session_voice_answer_grading_label
                                VoiceAnswerPhase.SpeakingNotice -> R.string.study_session_voice_answer_feedback_label
                                else -> R.string.study_session_voice_answer_listening_label
                            }
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.voiceAnswerPhase == VoiceAnswerPhase.SpeechDetected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (state.studyMode == StudyMode.Rated) {
                    IconButton(onClick = onVoiceAnswerToggle) {
                        Icon(
                            imageVector = if (state.isVoiceAnswerEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = stringResource(
                                if (state.isVoiceAnswerEnabled) {
                                    R.string.study_session_voice_answer_disable_cd
                                } else {
                                    R.string.study_session_voice_answer_enable_cd
                                }
                            ),
                            tint = if (state.isVoiceAnswerEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                IconButton(onClick = onVoiceSettingsCogClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Voice settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Shown as soon as the sanitized transcript streams in (ADR-0028) — screen-on is a
            // first-class case, not just a background/audio-only fallback, so the transcript
            // should be readable the moment it arrives rather than waiting for the grade.
            if (state.studyMode == StudyMode.Rated &&
                state.isVoiceAnswerEnabled &&
                !state.voiceAnswerSanitizedTranscript.isNullOrBlank() &&
                (state.voiceAnswerPhase == VoiceAnswerPhase.Grading || state.voiceAnswerPhase == VoiceAnswerPhase.SpeakingNotice)
            ) {
                Text(
                    text = state.voiceAnswerSanitizedTranscript,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            ) {
                // While voice-answering is actively listening/grading/speaking feedback, manual
                // skip controls must stay disabled: skipping to the answer here would start
                // TtsPlayer reading the answer aloud while VoiceAnswerController's mic is still
                // hot (grading the TTS's own voice), and skipping during SPEAKING_NOTICE would
                // start the next question on the main TTS engine while VoiceAnswerController's
                // separate notice engine is still talking — two overlapping voices.
                val isVoiceAnswerBusy = state.isVoiceAnswerEnabled &&
                    state.voiceAnswerPhase in setOf(
                        VoiceAnswerPhase.Listening,
                        VoiceAnswerPhase.SpeechDetected,
                        VoiceAnswerPhase.Grading,
                        VoiceAnswerPhase.SpeakingNotice
                    )
                // Pause only needs to stay disabled for the narrower "answer listening" window —
                // it toggles the main TtsPlayer, which is a no-op while the mic is what's actually
                // capturing (LISTENING/SPEECH_DETECTED); re-enables the moment the answer (or its
                // absence) has been noted and GRADING/SPEAKING_NOTICE takes over.
                val isVoiceAnswerListening = state.isVoiceAnswerEnabled &&
                    state.voiceAnswerPhase in setOf(
                        VoiceAnswerPhase.Listening,
                        VoiceAnswerPhase.SpeechDetected
                    )
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onVoicePrevious,
                        enabled = state.currentCardIndex > 0 && !isVoiceAnswerBusy,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous card",
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    FilledIconButton(
                        onClick = onVoicePlayPause,
                        modifier = Modifier.size(56.dp),
                        enabled = !isVoiceAnswerListening,
                    ) {
                        Icon(
                            imageVector = if (state.isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isVoicePlaying) "Pause" else "Play",
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    IconButton(
                        // "Show answer" only makes sense in manual Rated mode. In voice-answering
                        // mode the answer is revealed by the grading pipeline itself (ADR-0026),
                        // never by this button — here it can only mean "skip this question".
                        onClick = if (state.isVoiceAnswerEnabled || state.isAnswerRevealed) {
                            onVoiceNext
                        } else {
                            onShowAnswer
                        },
                        enabled = !isVoiceAnswerBusy,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = stringResource(
                                if (state.isVoiceAnswerEnabled || state.isAnswerRevealed) {
                                    R.string.study_session_next_flashcard_cd
                                } else {
                                    R.string.study_session_show_answer_cd
                                }
                            ),
                        )
                    }
                }
            }
        } else {
            if (state.studyMode == StudyMode.Rated) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.study_session_voice_answer_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onVoiceAnswerToggle) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = stringResource(R.string.study_session_voice_answer_enable_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!state.isAnswerRevealed) {
                Button(
                    onClick = onShowAnswer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Show Answer")
                }
            } else {
                RatingButtons(onRating = onRating)
            }
        }
    }
}

@Composable
private fun RatingButtons(
    onRating: (FlashcardRating) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "How well did you answer it?",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            RatingOption(
                label = "Not at all",
                icon = Icons.Default.Close,
                containerColor = MaterialTheme.semanticColors.negativeContainer,
                contentColor = MaterialTheme.semanticColors.onNegativeContainer,
                onClick = { onRating(FlashcardRating.Failed) },
            )
            RatingOption(
                label = "Somewhat",
                icon = Icons.Default.Remove,
                containerColor = MaterialTheme.semanticColors.neutralContainer,
                contentColor = MaterialTheme.semanticColors.onNeutralContainer,
                onClick = { onRating(FlashcardRating.PartiallyCorrect) },
            )
            RatingOption(
                label = "Very well",
                icon = Icons.Default.Check,
                containerColor = MaterialTheme.semanticColors.positiveContainer,
                contentColor = MaterialTheme.semanticColors.onPositiveContainer,
                onClick = { onRating(FlashcardRating.Correct) },
            )
        }
    }
}

@Composable
private fun RatingOption(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CenteredBox(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun StudySessionVoiceActivePreview() {
    StudySessionContent(
        state = StudySessionScreenState(
            sessionTitle = "Compose",
            flashcards = emptyList(),
            isVoiceActive = true,
            isVoicePlaying = true,
            speechRate = 1.25f,
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onNavigateBack = {},
        onShowAnswer = {},
        onNextCard = {},
        onRating = {},
        onVoicePlayPause = {},
        onVoiceNext = {},
        onVoicePrevious = {},
        onVoiceAnswerToggle = {},
        onDialogEvent = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun StudySessionRatedManualPreview() {
    StudySessionContent(
        state = StudySessionScreenState(
            sessionTitle = "Compose",
            flashcards = listOf(
                com.rossomak.flashcards.core.domain.model.Flashcard(
                    id = "1",
                    subcategoryId = "compose",
                    tags = emptyList(),
                    question = "What is the difference between remember and rememberSaveable?",
                    answer = "remember persists state across recompositions only; rememberSaveable also survives " +
                        "configuration changes and process death by storing in a Bundle.",
                    difficulty = 2,
                    questionCode = null,
                    answerCode = null,
                    questionSpoken = null,
                    answerSpoken = null,
                    extendedContext = null,
                ),
            ),
            isAnswerRevealed = true,
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onNavigateBack = {},
        onShowAnswer = {},
        onNextCard = {},
        onRating = {},
        onVoicePlayPause = {},
        onVoiceNext = {},
        onVoicePrevious = {},
        onVoiceAnswerToggle = {},
        onDialogEvent = {},
    )
}
