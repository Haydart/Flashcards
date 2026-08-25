package com.rossomak.flashcards.feature.onboarding.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.onboarding.R
import com.rossomak.flashcards.feature.onboarding.component.FavoriteTopicCard
import com.rossomak.flashcards.feature.onboarding.component.OnboardingStepHeader
import com.rossomak.flashcards.feature.onboarding.model.FavoriteTopicOption
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

private const val FAVORITE_GRID_COLUMNS = 2

/**
 * Height of the topic grid — about 3.5 rows, so a peek of the next row shows under the bottom
 * fade and hints the grid scrolls, rather than sizing to whatever fits the remaining screen. An
 * unbounded grid here was the header's problem, not the grid's: the pager centers each step's
 * content as one block, so a full-height grid inflated that block tall enough to crowd the header
 * up against the segmented progress bar above it.
 */
private val FavoriteGridHeight = 372.dp

/** Portion of [FavoriteGridHeight], from the bottom, over which the grid fades to transparent. */
private val FavoriteGridFadeHeight = 56.dp

/**
 * Lets the user pin topics for quick access from Home.
 *
 * Unlike every other step this one scrolls a grid rather than a column, so it hosts its own
 * [LazyVerticalGrid] instead of the shared [com.rossomak.flashcards.feature.onboarding.component.OnboardingStepColumn].
 */
@Composable
internal fun FavoritesStep(
    options: ImmutableList<FavoriteTopicOption>,
    selectedIds: ImmutableSet<String>,
    onTopicToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OnboardingStepHeader(
            eyebrow = stringResource(R.string.favorites_eyebrow_label),
            headline = stringResource(R.string.favorites_headline_title),
            message = stringResource(R.string.favorites_intro_message),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        LazyVerticalGrid(
            columns = GridCells.Fixed(FAVORITE_GRID_COLUMNS),
            modifier = Modifier
                .height(FavoriteGridHeight)
                .bottomFade(FavoriteGridFadeHeight),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
        ) {
            items(items = options, key = { option -> option.id }) { option ->
                FavoriteTopicCard(
                    name = option.name,
                    categoryName = option.categoryName,
                    icon = option.categoryName.categoryIcon(),
                    selected = option.id in selectedIds,
                    onSelectedChange = { onTopicToggle(option.id) },
                )
            }
        }
    }
}

/**
 * Fades this composable's own bottom [height] to transparent, revealing whatever sits behind it
 * (here, the screen's brand gradient) rather than painting a colour over it — so the cropped row
 * at the bottom of the grid reads as an intentional peek, not a hard clip or a mismatched overlay.
 *
 * Runs on an offscreen [graphicsLayer] and composites the fade with [BlendMode.DstIn], which keeps
 * source alpha where the mask is opaque and zeroes it where the mask is transparent — the standard
 * Compose recipe for a content fade, as opposed to [androidx.compose.foundation.background], which
 * would only ever add colour on top.
 */
private fun Modifier.bottomFade(height: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fadeStart = (size.height - height.toPx()).coerceAtLeast(0f)
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Black,
                    (fadeStart / size.height) to Color.Black,
                    1f to Color.Transparent,
                ),
            ),
            blendMode = BlendMode.DstIn,
        )
    }

/**
 * Placeholder glyph mapping. Categories carry their own remote icon (`Category.iconSvg`), but this
 * step runs on a hardcoded option list that has no Category attached yet.
 */
// TODO(favorites): drop this in favour of Category.iconSvg once real subcategories are fetched.
private fun String.categoryIcon(): ImageVector = when (this) {
    "Android" -> Icons.Default.Android
    "iOS" -> Icons.Default.PhoneIphone
    "Python" -> Icons.Default.DataObject
    else -> Icons.Default.Code
}
