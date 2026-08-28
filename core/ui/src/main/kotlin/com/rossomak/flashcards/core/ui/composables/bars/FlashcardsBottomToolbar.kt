package com.rossomak.flashcards.core.ui.composables.bars

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme

/**
 * The docked bottom toolbar of the details screens: secondary icon actions on the leading edge,
 * one primary CTA on the trailing edge.
 *
 * Square and full-bleed, per M3's docked-toolbar spec (`DockedToolbarTokens` is `CornerNone` and
 * carries no elevation token) — it separates from the list behind it by container color alone, not
 * by a shadow or a divider. Height comes from the stock [BottomAppBar]; the docked toolbar's own
 * 64dp is not reachable, since [BottomAppBar] fixes its content row to `BottomAppBarTokens`' 80dp.
 *
 * The CTA sits in [trailing] rather than [BottomAppBar]'s `floatingActionButton` slot: the design
 * calls for the brand gradient pill
 * ([com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton]), not an M3 FAB
 * with its own container color and elevation.
 *
 * The bar is always pinned — it holds the screen's primary action, so it never scrolls away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsBottomToolbar(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        actions()
        Spacer(modifier = Modifier.weight(1f))
        trailing()
    }
}

@ShowkaseComposable(name = "Bottom toolbar", group = "Bars")
@Composable
fun FlashcardsBottomToolbarShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsBottomToolbar(
                actions = {
                    BadgedBox(badge = { Badge() }) {
                        IconButton(onClick = {}) {
                            Icon(imageVector = Icons.Filled.FilterList, contentDescription = "Filter")
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add card")
                    }
                },
                trailing = {
                    FlashcardsFilledButton(
                        text = "Start session",
                        onClick = {},
                        size = FlashcardsComponentSize.Small,
                        icon = Icons.Filled.PlayArrow,
                    )
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsBottomToolbarPreview() {
    FlashcardsBottomToolbarShowcase()
}
