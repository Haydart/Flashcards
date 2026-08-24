package com.rossomak.flashcards.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsTextButton
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentStyle
import com.rossomak.flashcards.core.ui.composables.progress.FlashcardsSegmentedProgressBar
import com.rossomak.flashcards.core.ui.navigation.observeAsEvents
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.onboarding.step.AllSetStep
import com.rossomak.flashcards.feature.onboarding.step.DailyGoalStep
import com.rossomak.flashcards.feature.onboarding.step.FavoritesStep
import com.rossomak.flashcards.feature.onboarding.step.MasteryStep
import com.rossomak.flashcards.feature.onboarding.step.SessionModesStep
import com.rossomak.flashcards.feature.onboarding.step.StructureStep
import com.rossomak.flashcards.feature.onboarding.step.VoicePrivacyStep
import com.rossomak.flashcards.feature.onboarding.step.WelcomeStep
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
    onNavigateToMain: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    observeAsEvents(viewModel.events) { destination ->
        when (destination) {
            OnboardingDestination.Main -> onNavigateToMain()
        }
    }

    OnboardingContent(
        modifier = modifier,
        state = state,
        onStudyModeSelect = viewModel::onStudyModeSelect,
        onDailyGoalDecrement = viewModel::onDailyGoalDecrement,
        onDailyGoalIncrement = viewModel::onDailyGoalIncrement,
        onFavoriteTopicToggle = viewModel::onFavoriteTopicToggle,
        onFinish = viewModel::onFinish,
    )
}

/**
 * All eight steps in one [HorizontalPager], with the gradient, progress bar, Skip and the CTA held
 * outside it as fixed chrome — so a swipe moves only the content, never the frame around it.
 *
 * Skip does not leave the flow: it jumps to the final step, which is the single place preferences
 * are committed. That keeps one exit door instead of two code paths that both have to remember to
 * save.
 */
@Composable
private fun OnboardingContent(
    modifier: Modifier = Modifier,
    state: OnboardingScreenState,
    onStudyModeSelect: (StudyMode) -> Unit,
    onDailyGoalDecrement: () -> Unit,
    onDailyGoalIncrement: () -> Unit,
    onFavoriteTopicToggle: (String) -> Unit,
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState { OnboardingStep.entries.size }
    val coroutineScope = rememberCoroutineScope()
    val currentStep = OnboardingStep.atPage(pagerState.currentPage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.brandColors.screenGradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(vertical = MaterialTheme.spacing.small),
        ) {
            OnboardingTopBar(
                step = currentStep,
                // Instant rather than animated: animating a jump of up to seven pages would fling
                // the user through every screen they just chose to skip.
                onSkip = { coroutineScope.launch { pagerState.scrollToPage(OnboardingStep.entries.lastIndex) } },
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingStepPage(
                    step = OnboardingStep.atPage(page),
                    state = state,
                    onStudyModeSelect = onStudyModeSelect,
                    onDailyGoalDecrement = onDailyGoalDecrement,
                    onDailyGoalIncrement = onDailyGoalIncrement,
                    onFavoriteTopicToggle = onFavoriteTopicToggle,
                )
            }
            OnboardingCta(
                step = currentStep,
                enabled = !state.isCommitting,
                onClick = {
                    if (currentStep == OnboardingStep.LAST) {
                        onFinish()
                    } else {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingTopBar(
    step: OnboardingStep,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        // The Skip slot keeps its height on the final step, where there is nothing to skip to, so
        // the pager content below never shifts as the user moves between steps.
        Box(modifier = Modifier.height(MaterialTheme.sizes.buttonHeightSmall)) {
            if (step.showsSkip) {
                FlashcardsTextButton(
                    text = stringResource(R.string.onboarding_skip_button),
                    onClick = onSkip,
                    size = FlashcardsComponentSize.Small,
                    style = FlashcardsComponentStyle.OnGradient,
                )
            }
        }
        // Same reasoning for the progress bar, which the two bookend steps do not show.
        Box(modifier = Modifier.height(MaterialTheme.sizes.progressBarThicknessNormal)) {
            val progressIndex = step.progressIndex
            if (progressIndex != null) {
                FlashcardsSegmentedProgressBar(
                    segmentCount = OnboardingStep.PROGRESS_SEGMENT_COUNT,
                    filledSegmentCount = progressIndex + 1,
                    style = FlashcardsComponentStyle.OnGradient,
                )
            }
        }
    }
}

@Composable
private fun OnboardingCta(
    step: OnboardingStep,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (step) {
        OnboardingStep.Welcome -> stringResource(R.string.welcome_start_button)
        OnboardingStep.AllSet -> stringResource(R.string.all_set_start_button)
        else -> stringResource(R.string.onboarding_continue_button)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        FlashcardsFilledButton(
            text = label,
            onClick = onClick,
            enabled = enabled,
            style = FlashcardsComponentStyle.OnGradient,
        )
    }
}

@Composable
private fun OnboardingStepPage(
    step: OnboardingStep,
    state: OnboardingScreenState,
    onStudyModeSelect: (StudyMode) -> Unit,
    onDailyGoalDecrement: () -> Unit,
    onDailyGoalIncrement: () -> Unit,
    onFavoriteTopicToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (step) {
        OnboardingStep.Welcome -> WelcomeStep(modifier = modifier)
        OnboardingStep.Structure -> StructureStep(modifier = modifier)
        OnboardingStep.Mastery -> MasteryStep(modifier = modifier)
        OnboardingStep.SessionModes -> SessionModesStep(
            selectedStudyMode = state.defaultStudyMode,
            onStudyModeSelect = onStudyModeSelect,
            modifier = modifier,
        )
        OnboardingStep.DailyGoal -> DailyGoalStep(
            dailyGoalMinutes = state.dailyGoalMinutes,
            canDecrement = state.canDecrementDailyGoal,
            canIncrement = state.canIncrementDailyGoal,
            onDecrement = onDailyGoalDecrement,
            onIncrement = onDailyGoalIncrement,
            modifier = modifier,
        )
        OnboardingStep.VoicePrivacy -> VoicePrivacyStep(
            // TODO(voice): wire to the real capture + on-device obfuscation preview. Tapping does
            //  nothing today so this release ships no RECORD_AUDIO prompt during onboarding.
            onTestVoice = {},
            modifier = modifier,
        )
        OnboardingStep.Favorites -> FavoritesStep(
            options = state.favoriteTopicOptions,
            selectedIds = state.selectedFavoriteTopicIds,
            onTopicToggle = onFavoriteTopicToggle,
            modifier = modifier,
        )
        OnboardingStep.AllSet -> AllSetStep(
            userName = state.userName,
            defaultStudyMode = state.defaultStudyMode,
            dailyGoalMinutes = state.dailyGoalMinutes,
            favoriteCount = state.selectedFavoriteTopicIds.size,
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun OnboardingContentPreview() {
    FlashcardsTheme {
        OnboardingContent(
            state = remember { OnboardingScreenState(userName = "Radek") },
            onStudyModeSelect = {},
            onDailyGoalDecrement = {},
            onDailyGoalIncrement = {},
            onFavoriteTopicToggle = {},
            onFinish = {},
        )
    }
}
