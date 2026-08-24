package com.rossomak.flashcards.feature.onboarding.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.onboarding.R
import com.rossomak.flashcards.feature.onboarding.component.OnboardingContentColors
import com.rossomak.flashcards.feature.onboarding.component.OnboardingInfoBanner
import com.rossomak.flashcards.feature.onboarding.component.OnboardingStepColumn
import com.rossomak.flashcards.feature.onboarding.component.OnboardingStepHeader

/**
 * Introduces Voice Answering and the on-device privacy transform.
 *
 * The free/premium split is deliberate: capture and the obfuscation transform are free for
 * everyone, and only the AI grading of the spoken answer is gated — hence a premium line scoped to
 * grading rather than a badge over the whole screen.
 */
@Composable
internal fun VoicePrivacyStep(
    onTestVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepColumn(modifier = modifier) {
        OnboardingStepHeader(
            eyebrow = stringResource(R.string.voice_privacy_eyebrow_label),
            headline = stringResource(R.string.voice_privacy_headline_title),
            message = stringResource(R.string.voice_privacy_intro_message),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xsmall))
        PremiumNote()
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        VoiceTestCard(onTestVoice = onTestVoice)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.normal))
        OnboardingInfoBanner(
            text = stringResource(R.string.voice_privacy_banner_message),
            icon = Icons.Default.Lock,
        )
    }
}

@Composable
private fun PremiumNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxsmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.sizes.metadataBadgeIcon),
            tint = OnboardingContentColors.secondary,
        )
        Text(
            text = stringResource(R.string.voice_privacy_premium_message),
            style = MaterialTheme.typography.labelMedium,
            color = OnboardingContentColors.secondary,
        )
    }
}

@Composable
private fun VoiceTestCard(
    onTestVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.cornerRadius.card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.normal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Surface(
                modifier = Modifier.size(MaterialTheme.sizes.ratingButton),
                shape = RoundedCornerShape(MaterialTheme.cornerRadius.full),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                }
            }
            Text(
                text = stringResource(R.string.voice_privacy_try_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlashcardsFilledButton(
                text = stringResource(R.string.voice_privacy_test_button),
                onClick = onTestVoice,
                icon = Icons.Default.Mic,
            )
        }
    }
}
