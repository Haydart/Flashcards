package com.rossomak.flashcards.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

// TODO(core-ui): replace with the shared FlashcardsInfoBanner once it lands in :core:ui. This is a
//  local stand-in with the signature that component is expected to have, so adopting it should be
//  an import change at each call site rather than a rewrite.

/** Fill opacity of the banner against the brand screen gradient. */
private const val BANNER_CONTAINER_ALPHA = 0.14f

/** Border opacity — a hair brighter than the fill so the pill reads as raised, not cut out. */
private const val BANNER_BORDER_ALPHA = 0.22f

private val BannerIconSize = 18.dp

/**
 * Translucent pill carrying one secondary, supporting line beneath a step's main content. Sized and
 * weighted so it never competes with the primary content above it.
 */
@Composable
fun OnboardingInfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.EmojiEvents,
) {
    val shape = RoundedCornerShape(MaterialTheme.cornerRadius.large)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Color.White.copy(alpha = BANNER_CONTAINER_ALPHA), shape = shape)
            .border(
                width = MaterialTheme.sizes.hairline,
                color = Color.White.copy(alpha = BANNER_BORDER_ALPHA),
                shape = shape,
            )
            .padding(
                horizontal = MaterialTheme.spacing.normal,
                vertical = MaterialTheme.spacing.small,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(BannerIconSize),
            tint = Color.White,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
        )
    }
}
