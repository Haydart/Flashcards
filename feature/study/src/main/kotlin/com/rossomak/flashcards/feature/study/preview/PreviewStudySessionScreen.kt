package com.rossomak.flashcards.feature.study.preview

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.CardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.composables.CardSortOrderDialog
import com.rossomak.flashcards.core.ui.navigation.observeAsEvents
import com.rossomak.flashcards.feature.study.StudySessionRoute
import kotlin.math.roundToInt

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
        onSessionCardCountChange = viewModel::onSessionCardCountChange,
        onDifficultyRangeChange = viewModel::onDifficultyRangeChange,
        onStudyModeSelect = viewModel::onStudyModeSelect,
        onSortDialogShow = viewModel::onSortDialogShow,
        onSortDialogDismiss = viewModel::onSortDialogDismiss,
        onSortOrderSelect = viewModel::onSortOrderSelect,
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
    onSessionCardCountChange: (Int) -> Unit,
    onDifficultyRangeChange: (IntRange) -> Unit,
    onStudyModeSelect: (StudyMode) -> Unit,
    onSortDialogShow: () -> Unit,
    onSortDialogDismiss: () -> Unit,
    onSortOrderSelect: (CardSortOrder) -> Unit,
    onStartSession: () -> Unit,
) {
    if (state.isSortDialogVisible) {
        CardSortOrderDialog(
            selectedSortOrder = state.sortOrder,
            showKeepAsDefaultOption = true,
            onSortOrderSelect = onSortOrderSelect,
            onDismiss = onSortDialogDismiss,
        )
    }
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
                onSessionCardCountChange = onSessionCardCountChange,
                onDifficultyRangeChange = onDifficultyRangeChange,
                onStudyModeSelect = onStudyModeSelect,
                onSortDialogShow = onSortDialogShow,
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
    onSessionCardCountChange: (Int) -> Unit,
    onDifficultyRangeChange: (IntRange) -> Unit,
    onStudyModeSelect: (StudyMode) -> Unit,
    onSortDialogShow: () -> Unit,
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
            AssistChip(onClick = onSortDialogShow, label = { Text(text = "Sort: ${sortOrderLabel(state.sortOrder)}") })
        }

        if (state.filterTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Filtered by ${state.filterTags.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SessionCardCountSlider(
            cardCount = state.sessionCardCount,
            onCardCountChange = onSessionCardCountChange,
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyRangeSlider(
            difficultyRange = state.difficultyRange,
            onDifficultyRangeChange = onDifficultyRangeChange,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choose study mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StudyModeCard(
                modifier = Modifier.weight(1f),
                title = "Rated",
                description = "Reveal each answer, then rate yourself. Progress is saved.",
                isSelected = state.selectedStudyMode == StudyMode.RATED,
                onSelect = { onStudyModeSelect(StudyMode.RATED) },
            )
            StudyModeCard(
                modifier = Modifier.weight(1f),
                title = "Fast",
                description = "Cards advance on a timer. Nothing is rated or saved.",
                isSelected = state.selectedStudyMode == StudyMode.FAST,
                onSelect = { onStudyModeSelect(StudyMode.FAST) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

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

@Composable
private fun StudyModeCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier.selectable(
            selected = isSelected,
            onClick = onSelect,
            role = Role.RadioButton,
        ),
        border = if (isSelected) {
            BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                RadioButton(selected = isSelected, onClick = null)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionCardCountSlider(
    modifier: Modifier = Modifier,
    cardCount: Int,
    onCardCountChange: (Int) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Session length",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$cardCount cards",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = cardCount.toFloat(),
            onValueChange = { onCardCountChange(it.roundToInt()) },
            valueRange = PreviewStudySessionScreenState.MIN_SESSION_CARD_COUNT.toFloat()..
                PreviewStudySessionScreenState.MAX_SESSION_CARD_COUNT.toFloat(),
            steps = PreviewStudySessionScreenState.MAX_SESSION_CARD_COUNT -
                PreviewStudySessionScreenState.MIN_SESSION_CARD_COUNT - 1,
        )
    }
}

@Composable
private fun DifficultyRangeSlider(
    modifier: Modifier = Modifier,
    difficultyRange: IntRange,
    onDifficultyRangeChange: (IntRange) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Difficulty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${difficultyRange.first}–${difficultyRange.last}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RangeSlider(
            value = difficultyRange.first.toFloat()..difficultyRange.last.toFloat(),
            onValueChange = { range ->
                onDifficultyRangeChange(range.start.roundToInt()..range.endInclusive.roundToInt())
            },
            valueRange = PreviewStudySessionScreenState.MIN_DIFFICULTY.toFloat()..
                PreviewStudySessionScreenState.MAX_DIFFICULTY.toFloat(),
            steps = PreviewStudySessionScreenState.MAX_DIFFICULTY -
                PreviewStudySessionScreenState.MIN_DIFFICULTY - 1,
        )
    }
}

// isQuickSession is checked before isSingleTopic so a quick session that happens to land on one
// topic still reads as "Quick session" rather than misreporting as a plain single-topic preview.
private fun screenTitle(state: PreviewStudySessionScreenState): String = when {
    state.isQuickSession -> "${state.categoryName} · Quick session"
    state.isSingleTopic -> "${state.categoryName} · ${state.subcategoryNames.first()}"
    else -> "${state.categoryName} · Custom session"
}

private fun sortOrderLabel(sortOrder: CardSortOrder): String = when (sortOrder) {
    CardSortOrder.DEFAULT -> "Default"
    CardSortOrder.EASIEST_FIRST -> "Easiest first"
    CardSortOrder.HARDEST_FIRST -> "Hardest first"
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
        onSessionCardCountChange = {},
        onDifficultyRangeChange = {},
        onStudyModeSelect = {},
        onSortDialogShow = {},
        onSortDialogDismiss = {},
        onSortOrderSelect = {},
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
        onSessionCardCountChange = {},
        onDifficultyRangeChange = {},
        onStudyModeSelect = {},
        onSortDialogShow = {},
        onSortDialogDismiss = {},
        onSortOrderSelect = {},
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
            filterTags = listOf("State Management", "Modifiers"),
            isLoading = false,
            selectedCardCount = 18,
            estimatedMinutes = 12,
        ),
        onNavigateBack = {},
        onRetry = {},
        onRerandomize = {},
        onSessionCardCountChange = {},
        onDifficultyRangeChange = {},
        onStudyModeSelect = {},
        onSortDialogShow = {},
        onSortDialogDismiss = {},
        onSortOrderSelect = {},
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
        onSessionCardCountChange = {},
        onDifficultyRangeChange = {},
        onStudyModeSelect = {},
        onSortDialogShow = {},
        onSortDialogDismiss = {},
        onSortOrderSelect = {},
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
            selectedCardCount = 12,
            estimatedMinutes = 8,
            selectedStudyMode = StudyMode.FAST,
        ),
        onNavigateBack = {},
        onRetry = {},
        onRerandomize = {},
        onSessionCardCountChange = {},
        onDifficultyRangeChange = {},
        onStudyModeSelect = {},
        onSortDialogShow = {},
        onSortDialogDismiss = {},
        onSortOrderSelect = {},
        onStartSession = {},
    )
}
