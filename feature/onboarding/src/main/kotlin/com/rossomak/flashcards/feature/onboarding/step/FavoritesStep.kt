package com.rossomak.flashcards.feature.onboarding.step

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.onboarding.R
import com.rossomak.flashcards.feature.onboarding.component.FavoriteTopicCard
import com.rossomak.flashcards.feature.onboarding.component.OnboardingStepHeader
import com.rossomak.flashcards.feature.onboarding.model.FavoriteTopicOption
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

private const val FAVORITE_GRID_COLUMNS = 2

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
    ) {
        OnboardingStepHeader(
            eyebrow = stringResource(R.string.favorites_eyebrow_label),
            headline = stringResource(R.string.favorites_headline_title),
            message = stringResource(R.string.favorites_intro_message),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        LazyVerticalGrid(
            columns = GridCells.Fixed(FAVORITE_GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
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
