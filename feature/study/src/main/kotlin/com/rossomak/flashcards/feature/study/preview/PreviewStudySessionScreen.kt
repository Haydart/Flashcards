package com.rossomak.flashcards.feature.study.preview

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.ui.R as CoreUiR
import com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheet
import com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheetState
import com.rossomak.flashcards.core.ui.composables.FlashcardsEmptyState
import com.rossomak.flashcards.core.ui.composables.FlashcardsIconCircle
import com.rossomak.flashcards.core.ui.composables.FlashcardsMetadataBadge
import com.rossomak.flashcards.core.ui.composables.bars.FlashcardsGradientTopBar
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsIconButton
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsOutlinedButton
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsTonalButton
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentStyle.OnGradient
import com.rossomak.flashcards.core.ui.composables.dialogs.label
import com.rossomak.flashcards.core.ui.composables.rememberFlashcardsBottomSheetState
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.observeAsEvents
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.study.R
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.ReadAloud
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceAnswering
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 * The screen's brand identity: [MaterialTheme.brandColors.screenGradient] painted full-bleed behind
 * a transparent [Scaffold], with [FlashcardsGradientTopBar] bleeding it up behind the status bar for
 * free. Deliberately the brand's fixed gradient, not category-tinted — per-category colour arrives
 * with the unbuilt category icon-and-colour feature, and no route change is needed here to prepare
 * for it.
 *
 * Settings live behind a sheet hidden until asked for (ticket 07). Its open/closed value is
 * screen-local view state — [rememberSaveable] here, not [PreviewStudySessionScreenState] — since
 * it has no bearing on card selection and would only bloat that state with a UI-only flag.
 * [FlashcardsBottomSheet]'s own [SheetState][androidx.compose.material3.SheetState] is hoisted
 * alongside it, above the loading/error/ready `when` below, so both survive the loading flicker a
 * dialog confirm or a subcategory reshuffle briefly puts the screen through — the sheet reappears
 * exactly as the user left it rather than resetting. [initiallySettingsSheetOpen] exists solely so a
 * `@Preview` can render the sheet-open state; every real caller leaves it at its default.
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
    initiallySettingsSheetOpen: Boolean = false,
) {
    PreviewDialogHost(
        activeDialog = state.activeDialog,
        onDialogEvent = onDialogEvent,
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.brandColors.screenGradient),
        containerColor = Color.Transparent,
        topBar = {
            FlashcardsGradientTopBar(
                title = screenTitle(state),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.preview_session_close_cd),
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
                CircularProgressIndicator(color = MaterialTheme.brandColors.onGradientContent)
            }
            state.error != null -> ErrorContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                error = state.error,
                onRetry = onRetry,
            )
            else -> DockedReadyContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = state,
                initiallySettingsSheetOpen = initiallySettingsSheetOpen,
                onDialogEvent = onDialogEvent,
                onReshuffleSubcategories = onReshuffleSubcategories,
                onResetFilters = onResetFilters,
                onStartSession = onStartSession,
            )
        }
    }
}

/**
 * The ready-state's [Box]: [ReadyContent] with [SessionSettingsSheet] docked over it (ADR-0043's
 * pattern) rather than resizing it — so keeping [HeroActions][ReadyContent]'s CTAs clear of the
 * sheet takes tracking its actual, live screen position rather than reacting to a layout
 * constraint. Both edges are captured in root coordinates (not this [Box]'s own, which the sheet's
 * placement doesn't otherwise expose) and diffed into `reservedBottomPx`; 0 until the sheet's
 * content has been laid out at least once, so a closed/not-yet-measured sheet never steals any
 * space.
 *
 * Also owns the sheet's open/closed value and [FlashcardsBottomSheetState] — screen-local view
 * state, [rememberSaveable] rather than [PreviewStudySessionScreenState], since it has no bearing
 * on card selection and is only ever read by this subtree. A settings badge (ticket 09) opens the
 * sheet *and* the dialog for the value it names — but staggered, not together: the sheet slides up
 * first, the dialog fades in a beat later, since the reveal is how a user discovers the sheet
 * exists at all. [onOpenSettingsDialog] below sets the sheet open immediately and only delays the
 * dialog's own open event.
 *
 * Lifted out of [PreviewStudySessionContent] purely to keep that function under detekt's
 * `LongMethod`/`LongParameterList`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockedReadyContent(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    initiallySettingsSheetOpen: Boolean,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
    onReshuffleSubcategories: () -> Unit,
    onResetFilters: () -> Unit,
    onStartSession: () -> Unit,
) {
    var settingsSheetOpen by rememberSaveable { mutableStateOf(initiallySettingsSheetOpen) }
    val settingsSheetState = rememberFlashcardsBottomSheetState(initiallyExpanded = initiallySettingsSheetOpen)
    LaunchedEffect(settingsSheetOpen) {
        if (settingsSheetOpen) settingsSheetState.sheetState.show() else settingsSheetState.sheetState.hide()
    }

    val coroutineScope = rememberCoroutineScope()
    val onOpenSettingsDialog: (PreviewDialog) -> Unit = { dialog ->
        settingsSheetOpen = true
        coroutineScope.launch {
            delay(BADGE_DIALOG_STAGGER_DELAY_MS)
            onDialogEvent(Open(dialog))
        }
    }

    var containerBottomInRootPx by remember { mutableFloatStateOf(0f) }
    var sheetTopInRootPx by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            containerBottomInRootPx = coordinates.positionInRoot().y + coordinates.size.height
        },
    ) {
        ReadyContent(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onOpenSettings = { settingsSheetOpen = true },
            onOpenSettingsDialog = onOpenSettingsDialog,
            onReshuffleSubcategories = onReshuffleSubcategories,
            onResetFilters = onResetFilters,
            onStartSession = onStartSession,
            reservedBottomPx = (containerBottomInRootPx - sheetTopInRootPx).coerceIn(0f, containerBottomInRootPx),
        )
        SessionSettingsSheet(
            state = state,
            sheetState = settingsSheetState,
            onDismissRequest = { settingsSheetOpen = false },
            onDialogEvent = onDialogEvent,
            onContentTopChanged = { sheetTopInRootPx = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.brandColors.onGradientContent,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.normal))
        FlashcardsOutlinedButton(
            text = stringResource(CoreUiR.string.common_retry_button),
            onClick = onRetry,
            style = OnGradient,
        )
    }
}

/**
 * The ready-state body: the hero (play circle, title, scope sentence and badges — or, once the
 * pool is empty, [FlashcardsEmptyState] in its place) above [HeroActions], which stays put either
 * way so Reshuffle topics remains reachable even from the empty state — a fresh sample can turn
 * an empty result into a match. Layout of the two is [AdaptiveHero]'s job.
 */
@Composable
private fun ReadyContent(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onOpenSettings: () -> Unit,
    onOpenSettingsDialog: (PreviewDialog) -> Unit,
    onReshuffleSubcategories: () -> Unit,
    onResetFilters: () -> Unit,
    onStartSession: () -> Unit,
    reservedBottomPx: Float = 0f,
) {
    val isEmpty = state.selectedCardCount == 0

    AdaptiveHero(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.medium),
        reservedBottomPx = reservedBottomPx,
        heroTop = { if (!isEmpty) HeroTop() },
        heroBody = {
            if (isEmpty) {
                EmptyHeroBody(onResetFilters = onResetFilters, onOpenSettings = onOpenSettings)
            } else {
                ScopeHeroBody(state = state, onOpenSettingsDialog = onOpenSettingsDialog)
            }
        },
        actions = {
            HeroActions(
                state = state,
                onOpenSettings = onOpenSettings,
                onReshuffleSubcategories = onReshuffleSubcategories,
                onStartSession = onStartSession,
            )
        },
    )
}

/** The play circle and "Ready to start?" title — the one part of the hero [AdaptiveHero] can drop. */
@Composable
private fun HeroTop(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FlashcardsIconCircle(
            icon = Icons.Default.PlayArrow,
            // Decorative: "Ready to start?" right below already names what this circle means.
            contentDescription = null,
            style = OnGradient,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.normal))
        Text(
            // titleMedium, not a headline style: matches FlashcardsEmptyState's own title size —
            // this hero is that empty state's populated counterpart in the very same layout slot
            // (see ReadyContent), so the two should read as the same weight of "state title".
            text = stringResource(R.string.preview_session_ready_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.brandColors.onGradientContent,
        )
    }
}

/**
 * The scope sentence and its read-only badges — minutes, cards, then topics for a multi-subcategory
 * or Quick session (today's code emitted topics before cards; the design calls for this order) —
 * plus [SettingsBadgeRow] on its own line beneath them. This half of the hero always survives
 * [AdaptiveHero]'s adaptation.
 */
@Composable
private fun ScopeHeroBody(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onOpenSettingsDialog: (PreviewDialog) -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            // bodyMedium, matching FlashcardsEmptyState's own supportingText size (see HeroTop).
            text = scopeDescription(state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.brandColors.onGradientContent,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.normal))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
        ) {
            FlashcardsMetadataBadge(
                label = stringResource(R.string.preview_session_estimated_minutes_badge_label, state.estimatedMinutes),
                icon = Icons.Default.Schedule,
                style = OnGradient,
            )
            FlashcardsMetadataBadge(
                label = pluralStringResource(
                    CoreUiR.plurals.session_length_cards_label,
                    state.selectedCardCount,
                    state.selectedCardCount,
                ),
                icon = Icons.Default.Style,
                style = OnGradient,
            )
            if (!state.isSingleSubcategory) {
                FlashcardsMetadataBadge(
                    label = pluralStringResource(
                        R.plurals.preview_session_topics_badge_label,
                        state.subcategoryCount,
                        state.subcategoryCount,
                    ),
                    icon = Icons.Default.List,
                    style = OnGradient,
                )
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xsmall))
        SettingsBadgeRow(state = state, onOpenSettingsDialog = onOpenSettingsDialog)
    }
}

/**
 * Mode and interaction, tappable — a new user's only on-screen evidence that either is a choice at
 * all, now that the settings sheet defaults to hidden (ticket 07). Kept off the read-only scope
 * badges' own row: a tappable pill sitting among read-only ones is poor affordance and worse
 * accessibility (ticket 09). Each [FlashcardsMetadataBadge] gets a non-null `onClick`, which is what
 * makes it announce itself as a button rather than static text — the scope badges above pass none.
 *
 * The second badge always names a behaviour, never its absence — "Manual", not "Off" — a deliberate
 * reversal of the usual rule against badging negatives (ticket 09).
 */
@Composable
private fun SettingsBadgeRow(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onOpenSettingsDialog: (PreviewDialog) -> Unit,
) {
    val isRated = state.config.mode == StudyMode.Rated
    val interactionEnabled = if (isRated) state.config.voiceAnsweringEnabled else state.config.readAloudEnabled

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
    ) {
        FlashcardsMetadataBadge(
            label = state.config.mode.label(),
            icon = if (isRated) Icons.Default.Star else Icons.Default.Bolt,
            style = OnGradient,
            onClick = { onOpenSettingsDialog(Mode(draft = state.config.mode)) },
        )
        val interactionBadge = interactionBadgeContent(isRated = isRated, enabled = interactionEnabled)
        FlashcardsMetadataBadge(
            label = interactionBadge.label,
            icon = interactionBadge.icon,
            style = OnGradient,
            onClick = {
                onOpenSettingsDialog(
                    if (isRated) {
                        VoiceAnswering(draft = state.config.voiceAnsweringEnabled)
                    } else {
                        ReadAloud(draft = state.config.readAloudEnabled)
                    },
                )
            },
        )
    }
}

/** [SettingsBadgeRow]'s second badge: label plus a leading icon, together since both are picked by the same three-way classification. */
private data class InteractionBadgeContent(val label: String, val icon: ImageVector)

/**
 * "Voice" (Rated) or "Auto" (Fast) when [enabled], "Manual" either way when not — never "Off". The
 * icon mirrors what each value already carries in
 * [VoiceAnsweringDialog][com.rossomak.flashcards.core.ui.composables.dialogs.VoiceAnsweringDialog]/
 * [ReadAloudDialog][com.rossomak.flashcards.core.ui.composables.dialogs.ReadAloudDialog].
 */
@Composable
private fun interactionBadgeContent(isRated: Boolean, enabled: Boolean): InteractionBadgeContent = when {
    !enabled -> InteractionBadgeContent(
        label = stringResource(R.string.preview_session_interaction_manual_label),
        icon = Icons.Default.TouchApp,
    )
    isRated -> InteractionBadgeContent(
        label = stringResource(R.string.preview_session_interaction_voice_label),
        icon = Icons.Default.Mic,
    )
    else -> InteractionBadgeContent(
        label = stringResource(R.string.preview_session_interaction_auto_label),
        icon = Icons.AutoMirrored.Filled.VolumeUp,
    )
}

/**
 * Ticket 10's nothing-matches state: [FlashcardsEmptyState] replaces the *whole* hero above it (no
 * play circle, no title, no scope sentence, no badges — [AdaptiveHero] never even composes
 * [HeroTop] when [ReadyContent] finds the pool empty), with two actions of its own. Reset filters,
 * primary, restores what the screen was originally handed; Session settings, secondary, opens the
 * sheet so the user can change what they're asking for rather than only undo it. Reshuffle is
 * deliberately absent from this pair — [HeroActions] itself keeps it off in the empty state — so
 * this state offers exactly two ways forward, not three.
 */
@Composable
private fun EmptyHeroBody(
    modifier: Modifier = Modifier,
    onResetFilters: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    FlashcardsEmptyState(
        modifier = modifier,
        icon = Icons.Default.SearchOff,
        title = stringResource(R.string.preview_session_empty_state_title),
        supportingText = stringResource(R.string.preview_session_empty_state_message),
        style = OnGradient,
        button = {
            FlashcardsFilledButton(
                text = stringResource(R.string.preview_session_empty_state_reset_button),
                onClick = onResetFilters,
                icon = Icons.Default.Refresh,
                style = OnGradient,
            )
        },
        secondaryButton = {
            FlashcardsOutlinedButton(
                text = stringResource(R.string.preview_session_empty_state_settings_button),
                onClick = onOpenSettings,
                icon = Icons.Default.Settings,
                style = OnGradient,
            )
        },
    )
}

/**
 * The unlabelled settings button and **Start session**, with **Reshuffle topics** full-width beneath
 * for Quick sessions only — except when nothing matches (ticket 10): reshuffling there is offered
 * nowhere, not just left off the empty state's own two actions, since a stale sample and a fresh one
 * look identical until reshuffled. Custom never offers it, single- or multi-subcategory alike: its
 * subcategories are hand-picked by the user, not sampled, so there is nothing to reshuffle
 * ([PreviewStudySessionScreenState.canReshuffleSubcategories]). Reshuffle stays enabled independent
 * of [PreviewStudySessionScreenState.canStart] otherwise: a fresh sample can turn an empty result
 * into a match, which is exactly when reshuffling is needed. Start's label never changes with the
 * sheet's open/closed value — the primary action's text must not shift under the user — and it stays
 * visible but disabled whenever nothing is selected, rather than disappearing.
 */
@Composable
private fun HeroActions(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onOpenSettings: () -> Unit,
    onReshuffleSubcategories: () -> Unit,
    onStartSession: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlashcardsIconButton(
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.preview_session_open_settings_cd),
                onClick = onOpenSettings,
                style = OnGradient,
            )
            FlashcardsFilledButton(
                text = stringResource(CoreUiR.string.common_start_session_button),
                onClick = onStartSession,
                enabled = state.canStart,
                icon = Icons.Default.PlayArrow,
                style = OnGradient,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.canReshuffleSubcategories && state.selectedCardCount > 0) {
            FlashcardsTonalButton(
                text = stringResource(R.string.preview_session_reshuffle_button),
                onClick = onReshuffleSubcategories,
                enabled = !state.isLoading,
                icon = Icons.Default.Shuffle,
                style = OnGradient,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Lays [heroBody] and [actions] out top-to-bottom-anchored — [actions] always sits flush with the
 * bottom of the *effective* height (the container's own height, less [reservedBottomPx]), and
 * [heroBody] always renders directly beneath [heroTop] — with whatever height is left over
 * absorbed as the gap between them. [heroTop] (the play circle and title) is the one element
 * allowed to disappear, and it is dropped as a whole rather than compared against a magic dp
 * cutoff: this measures all three slots first, and only places [heroTop] when its height, plus
 * [heroBody]'s and [actions]'s, actually fits the space on offer. [heroBody] and [actions] always
 * render, so the scope sentence, its badges, and the session's actions never disappear regardless
 * of available height.
 *
 * @param reservedBottomPx Live screen-space (px) the settings sheet currently covers at the
 *   bottom, so [actions] rises to sit just above it rather than being buried underneath — the
 *   sheet floats over this content rather than resizing it (ADR-0043's docking pattern), so
 *   nothing about this container's own constraints otherwise reflects the sheet being open. 0
 *   (the default) reserves nothing, matching a closed or not-yet-measured sheet.
 */
@Composable
private fun AdaptiveHero(
    modifier: Modifier = Modifier,
    reservedBottomPx: Float = 0f,
    heroTop: @Composable () -> Unit,
    heroBody: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    // Gap between HeroTop and the scope body, and the floor kept between the body and HeroActions.
    val heroSpacing = MaterialTheme.spacing.normal
    val actionsMinGap = MaterialTheme.spacing.medium
    // Breathing room kept above the hero and below the actions row (or the sheet's edge), replacing
    // what used to be this container's own vertical padding — folded in here instead, so it can be
    // netted against reservedBottomPx rather than stacking on top of it.
    val edgeMargin = MaterialTheme.spacing.medium

    SubcomposeLayout(modifier = modifier) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val heroSpacingPx = heroSpacing.roundToPx()
        val actionsMinGapPx = actionsMinGap.roundToPx()
        val edgeMarginPx = edgeMargin.roundToPx()

        val heroTopPlaceables = subcompose(AdaptiveHeroSlot.HeroTop, heroTop).map { it.measure(looseConstraints) }
        val heroBodyPlaceables = subcompose(AdaptiveHeroSlot.HeroBody, heroBody).map { it.measure(looseConstraints) }
        val actionsPlaceables = subcompose(AdaptiveHeroSlot.Actions, actions).map { it.measure(looseConstraints) }

        val heroTopHeight = heroTopPlaceables.sumOf { it.height }
        val heroBodyHeight = heroBodyPlaceables.sumOf { it.height }
        val actionsHeight = actionsPlaceables.sumOf { it.height }

        val effectiveBottom = (constraints.maxHeight - reservedBottomPx.roundToInt() - edgeMarginPx)
            .coerceAtLeast(edgeMarginPx)
        val availableForHero = effectiveBottom - edgeMarginPx
        val heightWithHeroTop = heroTopHeight + heroSpacingPx + heroBodyHeight + actionsMinGapPx + actionsHeight
        val showHeroTop = heroTopPlaceables.isNotEmpty() && heightWithHeroTop <= availableForHero

        layout(constraints.maxWidth, constraints.maxHeight) {
            var y = edgeMarginPx
            if (showHeroTop) {
                heroTopPlaceables.forEach { it.placeRelative((constraints.maxWidth - it.width) / 2, y) }
                y += heroTopHeight + heroSpacingPx
            }
            heroBodyPlaceables.forEach { it.placeRelative((constraints.maxWidth - it.width) / 2, y) }
            y += heroBodyHeight

            val actionsY = max(y + actionsMinGapPx, effectiveBottom - actionsHeight)
            actionsPlaceables.forEach { it.placeRelative((constraints.maxWidth - it.width) / 2, actionsY) }
        }
    }
}

private enum class AdaptiveHeroSlot { HeroTop, HeroBody, Actions }

/**
 * How long a settings badge tap waits after opening the sheet before opening its dialog (ticket
 * 09) — long enough that the sheet's own slide-up reads as a distinct event before the dialog (and
 * its ticket 04 background blur) covers it, short enough that the tap still feels like one action.
 * `BottomSheet`'s expand animation is spring-driven (see M3's `BottomSheet.kt`), not a fixed-duration
 * tween, so there is no single number to sync exactly against — this is tuned with headroom above a
 * typical settle, not measured from one.
 */
private const val BADGE_DIALOG_STAGGER_DELAY_MS = 300L

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
