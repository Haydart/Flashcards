package com.rossomak.flashcards.feature.onboarding.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.composables.FlashcardsIconTile
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.onboarding.R
import com.rossomak.flashcards.feature.onboarding.component.OnboardingInfoBanner
import com.rossomak.flashcards.feature.onboarding.component.OnboardingModeCard
import com.rossomak.flashcards.feature.onboarding.component.OnboardingStepColumn
import com.rossomak.flashcards.feature.onboarding.component.OnboardingStepHeader

/**
 * Explains Rated vs Fast and captures the user's default Study Mode in the same screen.
 *
 * Picking a mode here sets a persisted default only — it does not start anything. The Preview Study
 * Session Screen remains the only place a concrete session's mode is chosen (ADR-0030), which is
 * why this step's CTA is the flow's ordinary Continue and not a session-start button.
 */
@Composable
internal fun SessionModesStep(
    selectedStudyMode: StudyMode,
    onStudyModeSelect: (StudyMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepColumn(modifier = modifier) {
        OnboardingStepHeader(
            eyebrow = stringResource(R.string.session_modes_eyebrow_label),
            headline = stringResource(R.string.session_modes_headline_title),
            message = stringResource(R.string.session_modes_intro_message),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            OnboardingModeCard(
                title = stringResource(R.string.session_modes_rated_title),
                description = stringResource(R.string.session_modes_rated_message),
                selected = selectedStudyMode == StudyMode.Rated,
                onSelect = { onStudyModeSelect(StudyMode.Rated) },
                leading = { FlashcardsIconTile(icon = Icons.Default.Grading, contentDescription = null) },
            )
            OnboardingModeCard(
                title = stringResource(R.string.session_modes_fast_title),
                description = stringResource(R.string.session_modes_fast_message),
                selected = selectedStudyMode == StudyMode.Fast,
                onSelect = { onStudyModeSelect(StudyMode.Fast) },
                leading = { FlashcardsIconTile(icon = Icons.Default.Bolt, contentDescription = null) },
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.normal))
        OnboardingInfoBanner(text = stringResource(R.string.session_modes_hands_free_banner_message))
    }
}
