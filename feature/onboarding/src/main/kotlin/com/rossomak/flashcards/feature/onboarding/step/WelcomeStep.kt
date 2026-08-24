package com.rossomak.flashcards.feature.onboarding.step

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.onboarding.R
import com.rossomak.flashcards.feature.onboarding.component.OnboardingContentColors
import com.rossomak.flashcards.feature.onboarding.component.OnboardingStepColumn

private val LogoWidth = 200.dp
private val LogoHeight = LogoWidth * (1000f / 1800f)

/** The cover: brand mark, promise, and what the next few screens will cover. No decisions here. */
@Composable
internal fun WelcomeStep(modifier: Modifier = Modifier) {
    OnboardingStepColumn(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Image(
            painter = painterResource(R.drawable.flashcards_white),
            contentDescription = null,
            modifier = Modifier.size(width = LogoWidth, height = LogoHeight),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxlarge))
        Text(
            text = stringResource(R.string.welcome_headline_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = OnboardingContentColors.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.normal))
        Text(
            text = stringResource(R.string.welcome_intro_message),
            style = MaterialTheme.typography.bodyMedium,
            color = OnboardingContentColors.secondary,
            textAlign = TextAlign.Center,
        )
    }
}
