package com.rossomak.flashcards.feature.study.preview

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheet
import com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheetHeader
import com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheetState
import com.rossomak.flashcards.core.ui.composables.FlashcardsDifficultyRangePill
import com.rossomak.flashcards.core.ui.composables.FlashcardsEmptyState
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsIconButton
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters
import com.rossomak.flashcards.core.ui.composables.dialogs.label
import com.rossomak.flashcards.core.ui.composables.dialogs.readAloudLabel
import com.rossomak.flashcards.core.ui.composables.dialogs.speechRateLabel
import com.rossomak.flashcards.core.ui.composables.dialogs.voiceAnsweringLabel
import com.rossomak.flashcards.core.ui.composables.lists.FlashcardsSettingRow
import com.rossomak.flashcards.core.ui.composables.rememberFlashcardsBottomSheetState
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
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.SubcategoryCountRange
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
        onReshuffleSubcategories = viewModel::onReshuffleSubcategories,
        onDialogEvent = viewModel::onDialogEvent,
        onResetFilters = viewModel::onResetFilters,
        onStartSession = viewModel::onStartSession,
    )
}

/**
 * Settings live behind a sheet hidden until asked for (ticket 07). Its open/closed value is
 * screen-local view state — [rememberSaveable] here, not [PreviewStudySessionScreenState] — since
 * it has no bearing on card selection and would only bloat that state with a UI-only flag.
 * [FlashcardsBottomSheet]'s own [SheetState][androidx.compose.material3.SheetState] is hoisted
 * alongside it, above the loading/error/ready `when` below, so both survive the loading flicker a
 * dialog confirm or a subcategory reshuffle briefly puts the screen through — the sheet reappears
 * exactly as the user left it rather than resetting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewStudySessionContent(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onReshuffleSubcategories: () -> Unit,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
    onResetFilters: () -> Unit,
    onStartSession: () -> Unit,
) {
    PreviewDialogHost(
        activeDialog = state.activeDialog,
        onDialogEvent = onDialogEvent,
    )

    var settingsSheetOpen by rememberSaveable { mutableStateOf(false) }
    val settingsSheetState = rememberFlashcardsBottomSheetState()
    LaunchedEffect(settingsSheetOpen) {
        if (settingsSheetOpen) settingsSheetState.sheetState.show() else settingsSheetState.sheetState.hide()
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
                actions = {
                    // Temporary spot for the settings entry point — ticket 08 moves it into the
                    // on-gradient action row.
                    FlashcardsIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.preview_session_open_settings_cd),
                        onClick = { settingsSheetOpen = true },
                    )
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
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                ReadyContent(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onReshuffleSubcategories = onReshuffleSubcategories,
                    onResetFilters = onResetFilters,
                    onStartSession = onStartSession,
                )
                SessionSettingsSheet(
                    state = state,
                    sheetState = settingsSheetState,
                    onDismissRequest = { settingsSheetOpen = false },
                    onDialogEvent = onDialogEvent,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
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
    onReshuffleSubcategories: () -> Unit,
    onResetFilters: () -> Unit,
    onStartSession: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        if (state.selectedCardCount == 0) {
            FlashcardsEmptyState(
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.preview_session_empty_state_title),
                supportingText = stringResource(R.string.preview_session_empty_state_message),
                button = {
                    FlashcardsFilledButton(
                        text = stringResource(R.string.preview_session_empty_state_reset_button),
                        onClick = onResetFilters,
                        icon = Icons.Default.Refresh,
                    )
                },
            )
        } else {
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
                if (!state.isSingleSubcategory) {
                    AssistChip(onClick = {}, label = { Text(text = "${state.subcategoryCount} topics") })
                }
                AssistChip(onClick = {}, label = { Text(text = "${state.selectedCardCount} cards") })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.canReshuffleSubcategories) {
                // Enabled independent of canStart: a new Subcategory sample or a loosened filter
                // might turn an empty result into a match, so reshuffling must stay reachable
                // through the empty state, not just once cards are already selected.
                FilledTonalButton(
                    onClick = onReshuffleSubcategories,
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Reshuffle topics")
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
 * The settings sheet itself: [FlashcardsBottomSheet] docked over the ready screen, hidden until
 * the top bar's settings button opens it (ticket 07). [sheetState] and its open/closed value are
 * owned by the caller — this composable only renders what's inside once shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSettingsSheet(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    sheetState: FlashcardsBottomSheetState,
    onDismissRequest: () -> Unit,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
) {
    FlashcardsBottomSheet(
        state = sheetState,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        FlashcardsBottomSheetHeader(
            title = stringResource(R.string.preview_session_settings_sheet_title),
            onClose = onDismissRequest,
        )
        // weight(fill = false), not fillMaxHeight: a short row set should keep the sheet's
        // today's compact, content-hugging height, only capping — and scrolling — once the rows
        // actually run out of room (ticket 07).
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            SessionSettingRows(state = state, onDialogEvent = onDialogEvent)
        }
    }
}

/**
 * One row per adjustable setting, each opening its own dialog (ADR-0030), through the shared
 * [FlashcardsSettingRow].
 *
 * Voice answering and attempts are Rated-only: Fast mode has no rating step for either to drive
 * (ADR-0025). Read-aloud is the Fast-only counterpart — voice *output* plus hands-free advance,
 * where voice answering is voice *input*. Each is not offered outside its mode rather than being
 * offered and ignored, so the two branches are exclusive. The Voice row itself only joins them
 * when something will actually speak: Fast with read-aloud on, or voice answering on — Fast alone
 * has nothing to configure yet, since read-aloud might still be off.
 */
@Composable
private fun SessionSettingRows(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlashcardsSettingRow(
            label = stringResource(R.string.preview_session_mode_label),
            valueText = state.config.mode.label(),
            onClick = { onDialogEvent(Open(Mode(draft = state.config.mode))) },
        )
        if (state.config.mode == StudyMode.Rated) {
            FlashcardsSettingRow(
                label = stringResource(R.string.preview_session_voice_answering_label),
                valueText = voiceAnsweringLabel(state.config.voiceAnsweringEnabled),
                onClick = {
                    onDialogEvent(
                        Open(VoiceAnswering(draft = state.config.voiceAnsweringEnabled))
                    )
                },
            )
            FlashcardsSettingRow(
                label = stringResource(R.string.preview_session_attempts_label),
                valueText = pluralStringResource(
                    CoreUiR.plurals.rated_attempts_label,
                    state.config.ratedAttempts,
                    state.config.ratedAttempts,
                ),
                onClick = { onDialogEvent(Open(Attempts(draft = state.config.ratedAttempts))) },
            )
        } else {
            FlashcardsSettingRow(
                label = stringResource(R.string.preview_session_read_aloud_label),
                valueText = readAloudLabel(state.config.readAloudEnabled),
                onClick = { onDialogEvent(Open(ReadAloud(draft = state.config.readAloudEnabled))) },
            )
        }
        val fastModeSpeaksAloud = state.config.mode == StudyMode.Fast && state.config.readAloudEnabled
        if (fastModeSpeaksAloud || state.config.voiceAnsweringEnabled) {
            FlashcardsSettingRow(
                label = stringResource(R.string.preview_session_voice_settings_label),
                valueText = voicePlaybackSummary(state),
                onClick = { onDialogEvent(Open(VoiceSettings())) },
            )
        }
        FlashcardsSettingRow(
            label = stringResource(R.string.preview_session_length_label),
            valueText = pluralStringResource(
                CoreUiR.plurals.session_length_cards_label,
                state.config.length,
                state.config.length,
            ),
            onClick = { onDialogEvent(Open(Length(draft = state.config.length))) },
        )
        if (state.isQuickSession) {
            SubcategoryCountRangeSettingRow(state = state, onDialogEvent = onDialogEvent)
        }
        FlashcardsSettingRow(
            label = stringResource(R.string.preview_session_filters_label),
            value = { FiltersSettingValue(state = state) },
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
        FlashcardsSettingRow(
            label = stringResource(R.string.preview_session_sort_label),
            valueText = state.config.sortOrder.label(),
            onClick = { onDialogEvent(Open(Sort(draft = state.config.sortOrder))) },
        )
    }
}

/**
 * Lifted out of [SessionSettingRows] purely to keep that function under detekt's `LongMethod` — a
 * single-Subcategory or Custom session has nothing to sample and never shows this row (ADR-0040).
 */
@Composable
private fun SubcategoryCountRangeSettingRow(
    state: PreviewStudySessionScreenState,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
) {
    FlashcardsSettingRow(
        label = stringResource(R.string.preview_session_subcategory_count_range_label),
        valueText = stringResource(
            CoreUiR.string.subcategory_count_range_value_label,
            state.config.subcategoryCountRange.first,
            state.config.subcategoryCountRange.last,
        ),
        onClick = { onDialogEvent(Open(SubcategoryCountRange(draft = state.config.subcategoryCountRange))) },
    )
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
 * The Filters row's value slot: a tag count beside [FlashcardsDifficultyRangePill], rather than
 * the plain text every other row uses (ticket 07). Difficulty always renders; the tag count only
 * joins it when tags are actually narrowing the pool.
 *
 * `config.tagIds` is materialized to every available tag by default (mirroring SubcategoryDetails,
 * ADR-0038), so "narrowing" means it is a *proper* subset of [PreviewStudySessionScreenState.availableTags]
 * — comparing against emptiness alone would show a tag count even when nothing was narrowed.
 */
@Composable
private fun FiltersSettingValue(state: PreviewStudySessionScreenState) {
    val config = state.config
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (config.tagIds.isNotEmpty() && config.tagIds != state.availableTags.toSet()) {
            Text(
                text = pluralStringResource(
                    R.plurals.preview_session_filters_tags_value_label,
                    config.tagIds.size,
                    config.tagIds.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlashcardsDifficultyRangePill(
            lowLevel = config.difficultyRange.first,
            highLevel = config.difficultyRange.last,
        )
    }
}

// isQuickSession is checked before isSingleSubcategory so a quick session that happens to land on
// one subcategory still reads as "Quick session" rather than misreporting as a plain single-subcategory
// preview.
private fun screenTitle(state: PreviewStudySessionScreenState): String = when {
    state.isQuickSession -> "${state.categoryName} · Quick session"
    state.isSingleSubcategory -> "${state.categoryName} · ${state.subcategoryNames.first()}"
    else -> "${state.categoryName} · Custom session"
}

@Composable
private fun scopeDescription(state: PreviewStudySessionScreenState): AnnotatedString {
    val cardsText = pluralStringResource(
        CoreUiR.plurals.session_length_cards_label,
        state.selectedCardCount,
        state.selectedCardCount,
    )
    // Resolved here, not inside appendSubcategoryList, so that function can stay a plain (non-
    // @Composable) builder step — matching appendOxfordList — instead of tripping detekt/lint's
    // ComposableNaming rule for a lowercase Unit-returning @Composable.
    val otherSubcategoriesText = (state.subcategoryNames.size - SUBCATEGORY_LIST_VISIBLE_COUNT)
        .takeIf { state.subcategoryNames.size > SUBCATEGORY_LIST_TRUNCATION_THRESHOLD }
        ?.let { otherCount ->
            pluralStringResource(R.plurals.preview_session_scope_other_subcategories_count, otherCount, otherCount)
        }
    return buildAnnotatedString {
        fun appendBold(text: String) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }
        }

        when {
            state.isQuickSession -> {
                appendBold(cardsText)
                append(stringResource(R.string.preview_session_scope_quick_session_message))
                appendSubcategoryList(state.subcategoryNames, ::appendBold, otherSubcategoriesText)
                append(".")
            }
            state.isSingleSubcategory -> {
                appendBold(cardsText)
                append(stringResource(R.string.preview_session_scope_single_subcategory_prefix_message))
                appendBold(state.subcategoryNames.first())
                append(stringResource(R.string.preview_session_scope_single_subcategory_suffix_message))
            }
            else -> {
                appendBold(cardsText)
                append(stringResource(R.string.preview_session_scope_multi_subcategory_message))
                appendSubcategoryList(state.subcategoryNames, ::appendBold, otherSubcategoriesText)
                append(".")
            }
        }
    }
}

/**
 * Names past this count stop being useful to read at a glance and collapse into a count instead.
 * Pinned to [StudySessionConfig.MAX_SUBCATEGORY_COUNT] — Quick's subcategory-count cap — so a
 * Quick session, whose topics the user did not choose, making naming them the whole point of a
 * preview, never truncates. Custom sessions, unbounded, still collapse once they cross it.
 */
private val SUBCATEGORY_LIST_TRUNCATION_THRESHOLD = StudySessionConfig.MAX_SUBCATEGORY_COUNT
private const val SUBCATEGORY_LIST_VISIBLE_COUNT = 3

/**
 * Lists every Subcategory name in full when [otherSubcategoriesText] is `null` (fits within
 * [SUBCATEGORY_LIST_TRUNCATION_THRESHOLD]); otherwise the first [SUBCATEGORY_LIST_VISIBLE_COUNT]
 * plus that pre-resolved "and N other topics" summary of the rest.
 */
private fun AnnotatedString.Builder.appendSubcategoryList(
    names: List<String>,
    appendBold: (String) -> Unit,
    otherSubcategoriesText: String?,
) {
    if (otherSubcategoriesText == null) {
        appendOxfordList(names, appendBold)
        return
    }
    names.take(SUBCATEGORY_LIST_VISIBLE_COUNT).forEachIndexed { index, name ->
        if (index > 0) append(", ")
        appendBold(name)
    }
    append(otherSubcategoriesText)
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
        onReshuffleSubcategories = {},
        onDialogEvent = {},
        onResetFilters = {},
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
        onReshuffleSubcategories = {},
        onDialogEvent = {},
        onResetFilters = {},
        onStartSession = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewStudySessionEmptyStatePreview() {
    PreviewStudySessionContent(
        state = PreviewStudySessionScreenState(
            categoryName = "Android",
            subcategoryNames = listOf("Compose"),
            isLoading = false,
            config = StudySessionConfig(
                subcategoryIds = listOf("compose"),
                tagIds = setOf("state"),
                difficultyRange = 9..10,
            ),
            selectedCardCount = 0,
            estimatedMinutes = 0,
            availableTags = listOf("state", "modifiers", "recomposition"),
        ),
        onNavigateBack = {},
        onRetry = {},
        onReshuffleSubcategories = {},
        onDialogEvent = {},
        onResetFilters = {},
        onStartSession = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewStudySessionSingleSubcategoryPreview() {
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
        onReshuffleSubcategories = {},
        onDialogEvent = {},
        onResetFilters = {},
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
        onReshuffleSubcategories = {},
        onDialogEvent = {},
        onResetFilters = {},
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
        onReshuffleSubcategories = {},
        onDialogEvent = {},
        onResetFilters = {},
        onStartSession = {},
    )
}
