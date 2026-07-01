package com.rossomak.flashcards.feature.study.session

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallatinapps.syntaxmp.tokenizer.SyntaxTokenizer
import com.rossomak.flashcards.feature.study.BuildConfig
import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState
import com.rossomak.flashcards.core.ui.composables.SyntaxCodeBlock
import com.rossomak.flashcards.core.ui.composables.VoiceSettingsDialog
import com.rossomak.flashcards.core.ui.composables.withInlineCode
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun StudySessionScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudySessionViewModel = hiltViewModel(),
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
    ) { viewModel.onToggleVoiceMode() }

    val onToggleVoiceMode = {
        val needsNotificationPermission = !state.isVoiceActive &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onToggleVoiceMode()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

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
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onShowAnswer = viewModel::onShowAnswer,
        onNextCard = viewModel::onNextCard,
        onToggleVoiceMode = onToggleVoiceMode,
        onVoicePlayPause = viewModel::onVoicePlayPause,
        onVoiceNext = viewModel::onVoiceNext,
        onVoicePrevious = viewModel::onVoicePrevious,
        onVoiceSettingsCogClick = viewModel::onVoiceSettingsCogClick,
        onVoiceSettingsDraftVoiceChanged = viewModel::onVoiceSettingsDraftVoiceChanged,
        onVoiceSettingsDraftSpeedChanged = viewModel::onVoiceSettingsDraftSpeedChanged,
        onVoiceSettingsSave = viewModel::onVoiceSettingsSave,
        onVoiceSettingsDismiss = viewModel::onVoiceSettingsDismiss,
        onCurationFabClick = viewModel::onCurationFabClick,
        onCurationActionToggle = viewModel::onCurationActionToggle,
        onCurationDialogDismiss = viewModel::onCurationDialogDismiss,
        onExtendedContextDialogOpen = viewModel::onExtendedContextDialogOpen,
        onExtendedContextDialogDismissed = viewModel::onExtendedContextDialogDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionContent(
    state: StudySessionScreenState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onShowAnswer: () -> Unit,
    onNextCard: () -> Unit,
    onToggleVoiceMode: () -> Unit,
    onVoicePlayPause: () -> Unit,
    onVoiceNext: () -> Unit,
    onVoicePrevious: () -> Unit,
    onVoiceSettingsCogClick: () -> Unit,
    onVoiceSettingsDraftVoiceChanged: (String?) -> Unit,
    onVoiceSettingsDraftSpeedChanged: (Float) -> Unit,
    onVoiceSettingsSave: () -> Unit,
    onVoiceSettingsDismiss: () -> Unit,
    onCurationFabClick: () -> Unit,
    onCurationActionToggle: (CurationAction) -> Unit,
    onCurationDialogDismiss: () -> Unit,
    onExtendedContextDialogOpen: () -> Unit,
    onExtendedContextDialogDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = true),
    )

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        sheetSwipeEnabled = false,
        sheetPeekHeight = if (state.isVoiceActive) 180.dp else 112.dp,
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
                onNextCard = onNextCard,
                onShowAnswer = onShowAnswer,
                onVoicePlayPause = onVoicePlayPause,
                onVoiceNext = onVoiceNext,
                onVoicePrevious = onVoicePrevious,
                onVoiceSettingsCogClick = onVoiceSettingsCogClick,
            )
            if (state.voiceSettingsState.isVisible) {
                VoiceSettingsDialog(
                    availableVoices = state.voiceSettingsState.availableVoices,
                    selectedVoiceId = state.voiceSettingsState.draftVoiceId,
                    speechRate = state.voiceSettingsState.draftSpeed,
                    onVoiceSelected = onVoiceSettingsDraftVoiceChanged,
                    onSpeedChanged = onVoiceSettingsDraftSpeedChanged,
                    onSave = onVoiceSettingsSave,
                    onDismiss = onVoiceSettingsDismiss,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CenteredBox(innerPadding) { CircularProgressIndicator() }
            state.error != null -> CenteredBox(innerPadding) { Text(text = state.error) }
            state.flashcards.isEmpty() -> CenteredBox(innerPadding) { Text(text = "No cards available") }
            else -> {
                val card = state.flashcards[state.currentCardIndex]
                var isExtendedContextDialogOpen by remember(card.id) { mutableStateOf(false) }
                val syntaxEngine = remember { SyntaxTokenizer() }

                if (isExtendedContextDialogOpen && !card.extendedContext.isNullOrBlank()) {
                    AlertDialog(
                        onDismissRequest = {
                            isExtendedContextDialogOpen = false
                            onExtendedContextDialogDismissed()
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                isExtendedContextDialogOpen = false
                                onExtendedContextDialogDismissed()
                            }) { Text("Close") }
                        },
                        title = { Text("Extended context") },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = card.extendedContext.orEmpty().withInlineCode(),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                    )
                }

                @OptIn(ExperimentalLayoutApi::class)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (card.tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            card.tags.forEach { tag ->
                                androidx.compose.material3.Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(6.dp, 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                                Spacer(modifier = Modifier.height(8.dp))
                                SuggestionChip(
                                    onClick = {
                                        isExtendedContextDialogOpen = true
                                        onExtendedContextDialogOpen()
                                    },
                                    label = { Text("Extended context") },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (BuildConfig.DEBUG && state.flashcards.getOrNull(state.currentCardIndex) != null) {
            FloatingActionButton(
                onClick = onCurationFabClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    ),
            ) {
                Icon(imageVector = Icons.Default.Build, contentDescription = "Curate card")
            }
        }
        } // end Box

        if (BuildConfig.DEBUG && state.isCurationDialogVisible) {
            val currentCard = state.flashcards.getOrNull(state.currentCardIndex)
            if (currentCard != null) {
                CurationDialog(
                    currentActions = state.curationRequests[currentCard.id]?.actions ?: emptyMap(),
                    onActionToggle = onCurationActionToggle,
                    onDismiss = onCurationDialogDismiss,
                )
            }
        }
    }
}

@Composable
private fun CurationDialog(
    currentActions: Map<CurationAction, Instant>,
    onActionToggle: (CurationAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Curate Card") },
        text = {
            Column {
                CurationAction.entries.forEach { action ->
                    val isActive = currentActions.containsKey(action)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActionToggle(action) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = action.dialogIcon(),
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = action.dialogLabel(),
                            modifier = Modifier.weight(1f),
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

private fun CurationAction.dialogIcon(): ImageVector = when (this) {
    CurationAction.DIFFICULTY_TOO_EASY -> Icons.Default.ArrowUpward
    CurationAction.DIFFICULTY_TOO_HARD -> Icons.Default.ArrowDownward
    CurationAction.DELETE -> Icons.Default.Delete
    CurationAction.BACKTICK_REDO -> Icons.Default.Code
    CurationAction.NEEDS_CODE_EXAMPLE -> Icons.Default.DataObject
    CurationAction.FULL_REDO -> Icons.Default.Refresh
}

private fun CurationAction.dialogLabel(): String = when (this) {
    CurationAction.DIFFICULTY_TOO_EASY -> "Too easy — raise difficulty"
    CurationAction.DIFFICULTY_TOO_HARD -> "Too hard — lower difficulty"
    CurationAction.DELETE -> "Delete — duplicate or worthless"
    CurationAction.BACKTICK_REDO -> "Fix backtick formatting"
    CurationAction.NEEDS_CODE_EXAMPLE -> "Needs code example"
    CurationAction.FULL_REDO -> "Full rewrite needed"
}

@Composable
private fun StudySessionSheetContent(
    state: StudySessionScreenState,
    onShowAnswer: () -> Unit,
    onNextCard: () -> Unit,
    onVoicePlayPause: () -> Unit,
    onVoiceNext: () -> Unit,
    onVoicePrevious: () -> Unit,
    onVoiceSettingsCogClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        if (state.isVoiceActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                IconButton(
                    onClick = onVoiceSettingsCogClick,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Voice settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onVoicePrevious,
                        enabled = state.currentCardIndex > 0,
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
                    ) {
                        Icon(
                            imageVector = if (state.isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isVoicePlaying) "Pause" else "Play",
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    IconButton(
                        onClick = if (!state.isAnswerRevealed) onShowAnswer else onVoiceNext,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = if (!state.isAnswerRevealed) "Show answer" else "Next card",
                        )
                    }
                }
            }
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
        snackbarHostState = remember { SnackbarHostState() },
        onNavigateBack = {},
        onShowAnswer = {},
        onNextCard = {},
        onToggleVoiceMode = {},
        onVoicePlayPause = {},
        onVoiceNext = {},
        onVoicePrevious = {},
        onVoiceSettingsCogClick = {},
        onVoiceSettingsDraftVoiceChanged = {},
        onVoiceSettingsDraftSpeedChanged = {},
        onVoiceSettingsSave = {},
        onVoiceSettingsDismiss = {},
        onCurationFabClick = {},
        onCurationActionToggle = {},
        onCurationDialogDismiss = {},
        onExtendedContextDialogOpen = {},
        onExtendedContextDialogDismissed = {},
    )
}
