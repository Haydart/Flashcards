package com.rossomak.flashcards.feature.study.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.ui.R as CoreUiR
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters
import com.rossomak.flashcards.core.ui.composables.dialogs.label
import com.rossomak.flashcards.core.ui.composables.dialogs.readAloudLabel
import com.rossomak.flashcards.core.ui.composables.dialogs.speechRateLabel
import com.rossomak.flashcards.core.ui.composables.dialogs.voiceAnsweringLabel
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.observeAsEvents
import com.rossomak.flashcards.feature.study.R
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Attempts
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Filters
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Length
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.ReadAloud
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Sort
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceAnswering
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceSettings

@Composable
fun PreviewStudySessionScreen(
    modifier: Modifier = Modifier,
    viewModel: PreviewStudySessionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToStudySession: (StudySessionRoute) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    observeAsEvents(viewModel.events) { destination ->
        when (destination) {
            is PreviewStudySessionDestination.StudySession -> onNavigateToStudySession(destination.route)
        }
    }

    PreviewStudySessionContent(
        modifier = modifier,
        state = state,
        onNavigateBack = onNavigateBack,
        onRetry = viewModel::onRetry,
        onRerandomize = viewModel::onRerandomize,
        onDialogEvent = viewModel::onDialogEvent,
        onStartSession = viewModel::onStartSession,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewStudySessionContent(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onRerandomize: () -> Unit,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
    onStartSession: () -> Unit,
) {
    PreviewDialogHost(
        activeDialog = state.activeDialog,
        onDialogEvent = onDialogEvent,
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = screenTitle(state)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close session preview",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> ErrorContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                error = state.error,
                onRetry = onRetry,
            )
            else -> ReadyContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = state,
                onRerandomize = onRerandomize,
                onDialogEvent = onDialogEvent,
                onStartSession = onStartSession,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    modifier: Modifier = Modifier,
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = error, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun ReadyContent(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onRerandomize: () -> Unit,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
    onStartSession: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ready to start?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = scopeDescription(state),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(onClick = {}, label = { Text(text = "~ ${state.estimatedMinutes} min") })
            if (!state.isSingleTopic) {
                AssistChip(onClick = {}, label = { Text(text = "${state.topicCount} topics") })
            }
            AssistChip(onClick = {}, label = { Text(text = "${state.selectedCardCount} cards") })
        }

        Spacer(modifier = Modifier.height(24.dp))

        SessionSettingRows(state = state, onDialogEvent = onDialogEvent)

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.canRerandomize) {
                FilledTonalButton(
                    onClick = onRerandomize,
                    enabled = state.canStart,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Re-randomize")
                }
            }
            Button(
                onClick = onStartSession,
                enabled = state.canStart,
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Start session")
            }
        }
    }
}

/**
 * One row per adjustable setting, each opening its own dialog (ADR-0030). The rows are a plain
 * column for now — the persistent bottom-sheet chrome the ADR describes is a later pass, and the
 * rows and their dialogs are what carry the behavior.
 *
 * Voice answering and attempts are Rated-only: Fast mode has no rating step for either to drive
 * (ADR-0025). Read-aloud is the Fast-only counterpart — voice *output* plus hands-free advance,
 * where voice answering is voice *input*. Each is not offered outside its mode rather than being
 * offered and ignored, so the two branches are exclusive.
 */
@Composable
private fun SessionSettingRows(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SessionSettingRow(
            label = stringResource(R.string.preview_session_mode_label),
            value = state.config.mode.label(),
            onClick = { onDialogEvent(Open(Mode(draft = state.config.mode))) },
        )
        if (state.config.mode == StudyMode.Rated) {
            SessionSettingRow(
                label = stringResource(R.string.preview_session_voice_answering_label),
                value = voiceAnsweringLabel(state.config.voiceAnsweringEnabled),
                onClick = {
                    onDialogEvent(
                        Open(VoiceAnswering(draft = state.config.voiceAnsweringEnabled))
                    )
                },
            )
            SessionSettingRow(
                label = stringResource(R.string.preview_session_attempts_label),
                value = pluralStringResource(
                    CoreUiR.plurals.rated_attempts_label,
                    state.config.ratedAttempts,
                    state.config.ratedAttempts,
                ),
                onClick = { onDialogEvent(Open(Attempts(draft = state.config.ratedAttempts))) },
            )
        } else {
            SessionSettingRow(
                label = stringResource(R.string.preview_session_read_aloud_label),
                value = readAloudLabel(state.config.readAloudEnabled),
                onClick = { onDialogEvent(Open(ReadAloud(draft = state.config.readAloudEnabled))) },
            )
        }
        // Fast mode always plays cards aloud; Rated only when voice answering is on — the same
        // gate the row itself opens into (ADR-0030).
        if (state.config.mode == StudyMode.Fast || state.config.voiceAnsweringEnabled) {
            SessionSettingRow(
                label = stringResource(R.string.preview_session_voice_settings_label),
                value = voicePlaybackSummary(state),
                onClick = { onDialogEvent(Open(VoiceSettings())) },
            )
        }
        SessionSettingRow(
            label = stringResource(R.string.preview_session_length_label),
            value = pluralStringResource(
                CoreUiR.plurals.session_length_cards_label,
                state.config.length,
                state.config.length,
            ),
            onClick = { onDialogEvent(Open(Length(draft = state.config.length))) },
        )
        SessionSettingRow(
            label = stringResource(R.string.preview_session_filters_label),
            value = filtersLabel(state),
            onClick = {
                onDialogEvent(
                    Open(
                        Filters(
                            draft = FlashcardFilters(
                                selectedTags = state.config.tagIds,
                                difficultyRange = state.config.difficultyRange,
                            ),
                            availableTags = state.availableTags,
                        )
                    )
                )
            },
        )
        SessionSettingRow(
            label = stringResource(R.string.preview_session_sort_label),
            value = state.config.sortOrder.label(),
            onClick = { onDialogEvent(Open(Sort(draft = state.config.sortOrder))) },
        )
    }
}

@Composable
private fun SessionSettingRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    onClickLabel = stringResource(R.string.preview_session_edit_setting_cd, label),
                )
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Name and rate, or the rate alone while the platform voice list has not arrived yet or no longer
 * contains the saved id (an engine can be uninstalled between runs).
 */
@Composable
private fun voicePlaybackSummary(state: PreviewStudySessionScreenState): String {
    val rateLabel = speechRateLabel(state.config.voiceSettings.speechRate)
    val voiceName = state.availableVoices
        .firstOrNull { it.id == state.config.voiceSettings.voiceId }
        ?.displayName
        ?: return rateLabel
    return stringResource(R.string.preview_session_setting_value_separator_label, voiceName, rateLabel)
}

/**
 * Difficulty always reads; the tag count only joins it when tags are actually narrowing the pool.
 *
 * `config.tagIds` is materialized to every available tag by default (mirroring SubcategoryDetails,
 * ADR-0038), so "narrowing" means it is a *proper* subset of [PreviewStudySessionScreenState.availableTags]
 * — comparing against emptiness alone would show a tag count even when nothing was narrowed.
 */
@Composable
private fun filtersLabel(state: PreviewStudySessionScreenState): String {
    val config = state.config
    val difficultyLabel = stringResource(
        R.string.preview_session_filters_difficulty_value_label,
        config.difficultyRange.first,
        config.difficultyRange.last,
    )
    if (config.tagIds.isEmpty() || config.tagIds == state.availableTags.toSet()) return difficultyLabel
    val tagsLabel = pluralStringResource(
        R.plurals.preview_session_filters_tags_value_label,
        config.tagIds.size,
        config.tagIds.size,
    )
    return stringResource(R.string.preview_session_setting_value_separator_label, tagsLabel, difficultyLabel)
}

// isQuickSession is checked before isSingleTopic so a quick session that happens to land on one
// topic still reads as "Quick session" rather than misreporting as a plain single-topic preview.
private fun screenTitle(state: PreviewStudySessionScreenState): String = when {
    state.isQuickSession -> "${state.categoryName} · Quick session"
    state.isSingleTopic -> "${state.categoryName} · ${state.subcategoryNames.first()}"
    else -> "${state.categoryName} · Custom session"
}

private fun scopeDescription(state: PreviewStudySessionScreenState): AnnotatedString = buildAnnotatedString {
    fun appendBold(text: String) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }
    }

    when {
        state.isQuickSession -> {
            appendBold("${state.selectedCardCount} cards")
            append(" auto-selected across ")
            appendBold("${state.topicCount} ${state.categoryName}")
            append(" topics.")
        }
        state.isSingleTopic -> {
            appendBold("${state.selectedCardCount} cards")
            append(" from your ${state.subcategoryNames.first()} deck.")
        }
        else -> {
            appendBold("${state.selectedCardCount} cards")
            append(" from ")
            appendOxfordList(state.subcategoryNames, ::appendBold)
            append(".")
        }
    }
}

// Joins names as "A and B" (2 items) or "A, B, and C" (3+ items), bolding each name.
private fun AnnotatedString.Builder.appendOxfordList(names: List<String>, appendBold: (String) -> Unit) {
    names.forEachIndexed { index, name ->
        when (index) {
            0 -> Unit
            names.lastIndex -> append(if (names.size > 2) ", and " else " and ")
            else -> append(", ")
        }
        appendBold(name)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStudySessionLoadingPreview() {
    PreviewStudySessionContent(
        state = PreviewStudySessionScreenState(
            categoryName = "Android",
            subcategoryNames = listOf("Compose"),
            isLoading = true,
        ),
        onNavigateBack = {},
        onRetry = {},
        onRerandomize = {},
        onDialogEvent = {},
        onStartSession = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewStudySessionErrorPreview() {
    PreviewStudySessionContent(
        state = PreviewStudySessionScreenState(
            categoryName = "Android",
            subcategoryNames = listOf("Compose"),
            isLoading = false,
            error = "Could not load flashcards",
        ),
        onNavigateBack = {},
        onRetry = {},
        onRerandomize = {},
        onDialogEvent = {},
        onStartSession = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewStudySessionSingleTopicPreview() {
    PreviewStudySessionContent(
        state = PreviewStudySessionScreenState(
            categoryName = "Android",
            subcategoryNames = listOf("Compose"),
            isLoading = false,
            config = StudySessionConfig(
                subcategoryIds = listOf("compose"),
                tagIds = setOf("state", "modifiers"),
                difficultyRange = 2..8,
            ),
            selectedCardCount = 18,
            estimatedMinutes = 12,
            availableTags = listOf("state", "modifiers", "recomposition"),
        ),
        onNavigateBack = {},
        onRetry = {},
        onRerandomize = {},
        onDialogEvent = {},
        onStartSession = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewStudySessionQuickSessionPreview() {
    PreviewStudySessionContent(
        state = PreviewStudySessionScreenState(
            categoryName = "Android",
            subcategoryNames = listOf("Compose", "Coroutines", "Architecture", "Testing", "Navigation"),
            isQuickSession = true,
            isLoading = false,
            selectedCardCount = 20,
            estimatedMinutes = 13,
        ),
        onNavigateBack = {},
        onRetry = {},
        onRerandomize = {},
        onDialogEvent = {},
        onStartSession = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewStudySessionCustomSessionPreview() {
    PreviewStudySessionContent(
        state = PreviewStudySessionScreenState(
            categoryName = "Android",
            subcategoryNames = listOf("Compose", "Coroutines", "Architecture"),
            isLoading = false,
            config = StudySessionConfig(subcategoryIds = listOf("a", "b", "c"), mode = StudyMode.Fast),
            selectedCardCount = 12,
            estimatedMinutes = 8,
        ),
        onNavigateBack = {},
        onRetry = {},
        onRerandomize = {},
        onDialogEvent = {},
        onStartSession = {},
    )
}
