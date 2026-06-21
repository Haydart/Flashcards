package com.rossomak.flashcards.presentation.studysession

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallatinapps.syntaxmp.tokenizer.SyntaxTokenizer
import com.rossomak.flashcards.service.VoicePlaybackState
import com.rossomak.flashcards.ui.text.SyntaxCodeBlock
import com.rossomak.flashcards.ui.text.withInlineCode

@Composable
fun StudySessionScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudySessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.onToggleVoiceMode() }

    val onToggleVoiceMode = {
        val needsNotificationPermission = !state.isVoiceActive &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onToggleVoiceMode()
        }
    }

    LaunchedEffect(state.isSessionComplete) {
        if (state.isSessionComplete) onNavigateBack()
    }

    StudySessionContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onShowAnswer = viewModel::onShowAnswer,
        onNextCard = viewModel::onNextCard,
        onToggleVoiceMode = onToggleVoiceMode,
        onVoicePlayPause = viewModel::onVoicePlayPause,
        onVoiceNext = viewModel::onVoiceNext,
        onVoicePrevious = viewModel::onVoicePrevious,
        onVoiceSpeedChange = viewModel::onVoiceSpeedChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionContent(
    state: StudySessionScreenState,
    onNavigateBack: () -> Unit,
    onShowAnswer: () -> Unit,
    onNextCard: () -> Unit,
    onToggleVoiceMode: () -> Unit,
    onVoicePlayPause: () -> Unit,
    onVoiceNext: () -> Unit,
    onVoicePrevious: () -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = true),
    )

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = false,
        sheetPeekHeight = if (state.isVoiceActive) 220.dp else 112.dp,
        topBar = {
            TopAppBar(
                title = { Text(text = state.subcategoryName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit session",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleVoiceMode) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = if (state.isVoiceActive) "Stop voice playback" else "Start voice playback",
                            tint = if (state.isVoiceActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
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
                onNextCard = onNextCard,
                onVoicePlayPause = onVoicePlayPause,
                onVoiceNext = onVoiceNext,
                onVoicePrevious = onVoicePrevious,
                onVoiceSpeedChange = onVoiceSpeedChange,
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> CenteredBox(innerPadding) { CircularProgressIndicator() }
            state.error != null -> CenteredBox(innerPadding) { Text(text = state.error) }
            state.flashcards.isEmpty() -> CenteredBox(innerPadding) { Text(text = "No cards available") }
            else -> {
                val card = state.flashcards[state.currentCardIndex]
                var isExtendedContextExpanded by remember(card.id) { mutableStateOf(false) }
                val syntaxEngine = remember { SyntaxTokenizer() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
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
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.question.withInlineCode(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    card.questionCode?.forEach { codeBlock ->
                        Spacer(modifier = Modifier.height(8.dp))
                        SyntaxCodeBlock(
                            code = codeBlock.code,
                            language = codeBlock.language,
                            engine = syntaxEngine,
                        )
                    }
                    AnimatedVisibility(
                        visible = state.isAnswerRevealed,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Answer",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
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
                                    engine = syntaxEngine,
                                )
                            }
                            if (!card.extendedContext.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isExtendedContextExpanded = !isExtendedContextExpanded }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Extended context",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        imageVector = if (isExtendedContextExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExtendedContextExpanded) "Collapse" else "Expand",
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isExtendedContextExpanded,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    Text(
                                        text = card.extendedContext.withInlineCode(),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudySessionSheetContent(
    state: StudySessionScreenState,
    onShowAnswer: () -> Unit,
    onNextCard: () -> Unit,
    onVoicePlayPause: () -> Unit,
    onVoiceNext: () -> Unit,
    onVoicePrevious: () -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        if (state.isVoiceActive) {
            if (!state.isAnswerRevealed) {
                Button(
                    onClick = onShowAnswer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Show Answer")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onVoicePrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous card",
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                FilledIconButton(
                    onClick = onVoicePlayPause,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = if (state.isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isVoicePlaying) "Pause" else "Play",
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                IconButton(onClick = onVoiceNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next card",
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Speed ${"%.2f".format(state.speechRate)}x",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = state.speechRate,
                onValueChange = onVoiceSpeedChange,
                valueRange = VoicePlaybackState.MIN_SPEECH_RATE..VoicePlaybackState.MAX_SPEECH_RATE,
            )
        } else {
            if (!state.isAnswerRevealed) {
                Button(
                    onClick = onShowAnswer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Show Answer")
                }
            } else {
                val isLastCard = state.currentCardIndex == state.flashcards.lastIndex
                OutlinedButton(
                    onClick = onNextCard,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isLastCard) "Finish" else "Next")
                }
            }
        }
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
            subcategoryName = "Compose",
            flashcards = emptyList(),
            isVoiceActive = true,
            isVoicePlaying = true,
            speechRate = 1.25f,
        ),
        onNavigateBack = {},
        onShowAnswer = {},
        onNextCard = {},
        onToggleVoiceMode = {},
        onVoicePlayPause = {},
        onVoiceNext = {},
        onVoicePrevious = {},
        onVoiceSpeedChange = {},
    )
}
