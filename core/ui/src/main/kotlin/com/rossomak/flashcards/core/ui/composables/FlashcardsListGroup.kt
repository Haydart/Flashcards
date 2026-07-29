// Grouping file: the FlashcardsListGroup composable is the API; FlashcardsListGroupScope is
// its supporting DSL receiver.
@file:Suppress("MatchingDeclarationName")

package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.hairlineBorder
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * Records the rows of a [FlashcardsListGroup]. Each `item { }` becomes one row; the group
 * lays them out in order and inserts a divider between adjacent rows.
 */
class FlashcardsListGroupScope internal constructor() {
    internal val rows = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        rows.add(content)
    }
}

/**
 * A bounded (non-lazy) list container: one rounded, 1px-bordered card wrapping a `Column` of
 * rows with automatic full-width dividers between them (never at the edges). For short,
 * fixed sets — settings sections, category lists, selection groups. Long, scrolling lists use
 * a `LazyColumn` with [flashcardsListItemShape] instead, so laziness stays in the screen.
 */
@Composable
fun FlashcardsListGroup(
    modifier: Modifier = Modifier,
    content: FlashcardsListGroupScope.() -> Unit,
) {
    val scope = FlashcardsListGroupScope().apply(content)
    val shape = RoundedCornerShape(MaterialTheme.cornerRadius.card)
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = MaterialTheme.hairlineBorder,
    ) {
        Column {
            scope.rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                row()
            }
        }
    }
}

/**
 * [SwitchDefaults.colors] using the app's secondary (purple) role for the "on" track, matching
 * the design's settings-switch. `secondary`/`onSecondary` is the same solid+white M3 pairing the
 * checkbox in [FlashcardsSelectableListRow] uses, and stays theme-adaptive for free.
 */
@Composable
private fun accentSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
    checkedTrackColor = MaterialTheme.colorScheme.secondary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
    uncheckedBorderColor = Color.Transparent,
)

@ShowkaseComposable(name = "List group — permissions", group = "Lists")
@Composable
fun FlashcardsListGroupShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsListGroup {
                item {
                    FlashcardsListRow(
                        title = "Notifications",
                        onClick = {},
                        subtitle = "Allowed",
                        leading = { FlashcardsIconTile(icon = Icons.Default.NotificationsActive, contentDescription = null) },
                        trailing = { FlashcardsChevron() },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Microphone",
                        onClick = {},
                        subtitle = "Not granted",
                        leading = { FlashcardsIconTile(icon = Icons.Default.Mic, contentDescription = null) },
                        trailing = { FlashcardsChevron() },
                    )
                }
            }
        }
    }
}

@ShowkaseComposable(name = "List group — study sessions", group = "Lists")
@Composable
fun FlashcardsListGroupStudySessionsShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsListGroup {
                item {
                    FlashcardsListRow(
                        title = "Session size",
                        onClick = {},
                        subtitle = "Cards per session",
                        leading = { FlashcardsIconTile(icon = Icons.Default.Tag, contentDescription = null) },
                        trailing = {
                            FlashcardsStepper(
                                value = 20,
                                onDecrement = {},
                                onIncrement = {},
                                decrementContentDescription = "Fewer cards",
                                incrementContentDescription = "More cards",
                            )
                        },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Auto-flip on swipe",
                        onClick = {},
                        subtitle = "Reveal answer with a single swipe",
                        leading = { FlashcardsIconTile(icon = Icons.Default.SwipeUp, contentDescription = null) },
                        trailing = { Switch(checked = false, onCheckedChange = null, colors = accentSwitchColors()) },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Shuffle order",
                        onClick = {},
                        subtitle = "Always randomise card order",
                        leading = { FlashcardsIconTile(icon = Icons.Default.Shuffle, contentDescription = null) },
                        trailing = { Switch(checked = true, onCheckedChange = null, colors = accentSwitchColors()) },
                    )
                }
            }
        }
    }
}

/**
 * Sample per-category colors, standing in for a future `Category.color` domain field. Each
 * category defines its own color independently — these are NOT sourced from `colorScheme`
 * roles, even where a value happens to coincide with one (Android's matches this app's brand
 * purple, but that's a coincidence of this particular category, not a theme reference).
 */
private val categoryColorAndroid = Color(0xFF6B2FA0)
private val categoryColorPython = Color(0xFF0277BD)
private val categoryColorIos = Color(0xFF00838F)
private const val CATEGORY_TILE_ALPHA = 0.12f

@ShowkaseComposable(name = "List group — categories", group = "Lists")
@Composable
fun FlashcardsListGroupCategoriesShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsListGroup {
                item {
                    FlashcardsListRow(
                        title = "Android",
                        onClick = {},
                        subtitle = "Compose · Coroutines · Compose Navigation",
                        secondaryText = "13 topics",
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.Android,
                                contentDescription = null,
                                containerColor = categoryColorAndroid.copy(alpha = CATEGORY_TILE_ALPHA),
                                contentColor = categoryColorAndroid,
                            )
                        },
                        trailing = { FlashcardsChevron() },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Python",
                        onClick = {},
                        subtitle = "Async · Typing · Decorators",
                        secondaryText = "6 topics",
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.Terminal,
                                contentDescription = null,
                                containerColor = categoryColorPython.copy(alpha = CATEGORY_TILE_ALPHA),
                                contentColor = categoryColorPython,
                            )
                        },
                        trailing = { FlashcardsChevron() },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "iOS",
                        onClick = {},
                        subtitle = "SwiftUI · Combine · UIKit",
                        secondaryText = "5 topics",
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.PhoneIphone,
                                contentDescription = null,
                                containerColor = categoryColorIos.copy(alpha = CATEGORY_TILE_ALPHA),
                                contentColor = categoryColorIos,
                            )
                        },
                        trailing = { FlashcardsChevron() },
                    )
                }
            }
        }
    }
}

@ShowkaseComposable(name = "List group — search results", group = "Lists")
@Composable
fun FlashcardsListGroupSearchResultsShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsListGroup {
                item {
                    FlashcardsListRow(
                        title = "Compose",
                        onClick = {},
                        secondaryText = "in Android",
                        leading = { FlashcardsAccentStripe(color = categoryColorAndroid) },
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall)) {
                                FlashcardsPlayButton(onClick = {}, contentDescription = "Study Compose")
                                FlashcardsChevron()
                            }
                        },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Compose Navigation",
                        onClick = {},
                        secondaryText = "in Android",
                        leading = { FlashcardsAccentStripe(color = categoryColorAndroid) },
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall)) {
                                FlashcardsPlayButton(onClick = {}, contentDescription = "Study Compose Navigation")
                                FlashcardsChevron()
                            }
                        },
                    )
                }
            }
        }
    }
}

@ShowkaseComposable(name = "List group — multi-select topics", group = "Lists")
@Composable
fun FlashcardsListGroupMultiSelectShowcase() {
    FlashcardsTheme {
        Surface {
            Column {
                FlashcardsSectionHeader(text = "3 of 13 topics selected")
                FlashcardsListGroup {
                    item {
                        FlashcardsSelectableListRow(
                            title = "Compose",
                            selected = true,
                            onSelectedChange = {},
                            subtitle = "80 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                    item {
                        FlashcardsSelectableListRow(
                            title = "Coroutines",
                            selected = true,
                            onSelectedChange = {},
                            subtitle = "40 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                    item {
                        FlashcardsSelectableListRow(
                            title = "Compose Navigation",
                            selected = false,
                            onSelectedChange = {},
                            subtitle = "30 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                    item {
                        FlashcardsSelectableListRow(
                            title = "Testing",
                            selected = false,
                            onSelectedChange = {},
                            subtitle = "25 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                    item {
                        FlashcardsSelectableListRow(
                            title = "Architecture",
                            selected = true,
                            onSelectedChange = {},
                            subtitle = "20 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsListGroupPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsListGroup {
                item {
                    FlashcardsListRow(
                        title = "Notifications",
                        onClick = {},
                        subtitle = "Allowed",
                        leading = { FlashcardsIconTile(icon = Icons.Default.NotificationsActive, contentDescription = null) },
                        trailing = { FlashcardsChevron() },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Microphone",
                        onClick = {},
                        subtitle = "Not granted",
                        leading = { FlashcardsIconTile(icon = Icons.Default.Mic, contentDescription = null) },
                        trailing = { FlashcardsChevron() },
                    )
                }
            }
        }
    }
}
