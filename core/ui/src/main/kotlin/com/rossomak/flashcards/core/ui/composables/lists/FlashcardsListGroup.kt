// Grouping file: the FlashcardsListGroup composable is the API; FlashcardsListGroupScope is
// its supporting DSL receiver.
@file:Suppress("MatchingDeclarationName")

package com.rossomak.flashcards.core.ui.composables.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.composables.FlashcardsIconTile
import com.rossomak.flashcards.core.ui.composables.FlashcardsOverlineLabel
import com.rossomak.flashcards.core.ui.composables.FlashcardsPlayButton
import com.rossomak.flashcards.core.ui.composables.FlashcardsStepper
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * Records the rows of a [FlashcardsListGroup]. Each `item { }` becomes one row; the group
 * lays them out in order and shapes each one with [flashcardsListItemShape], marking it
 * [checked] when the group is in multi-select mode and this row is selected.
 */
class FlashcardsListGroupScope internal constructor() {
    internal class Row(val checked: Boolean, val content: @Composable () -> Unit)

    internal val rows = mutableListOf<Row>()

    fun item(checked: Boolean = false, content: @Composable () -> Unit) {
        rows.add(Row(checked, content))
    }
}

/**
 * A bounded (non-lazy) list container: one rounded card wrapping a `Column` of rows, each
 * shaped by [flashcardsListItemShape] against the [flashcardsListGroupContainer] background
 * so interior seams read as a 1dp gap rather than a drawn divider. For short, fixed sets —
 * settings sections, category lists, selection groups. Long, scrolling lists use a
 * `LazyColumn` with [flashcardsListItemShape] directly instead, so laziness stays in the screen.
 */
@Composable
fun FlashcardsListGroup(
    modifier: Modifier = Modifier,
    content: FlashcardsListGroupScope.() -> Unit,
) {
    val scope = FlashcardsListGroupScope().apply(content)
    Column(modifier = modifier.flashcardsListGroupContainer()) {
        scope.rows.forEachIndexed { index, row ->
            val position = FlashcardsListItemPosition.of(index, scope.rows.size)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .flashcardsListItemShape(position, checked = row.checked),
            ) {
                row.content()
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
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.NotificationsActive,
                                contentDescription = null
                            )
                        },
                        trailing = { FlashcardsChevron() },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Microphone",
                        onClick = {},
                        subtitle = "Not granted",
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.Mic,
                                contentDescription = null
                            )
                        },
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
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.Tag,
                                contentDescription = null
                            )
                        },
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
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.SwipeUp,
                                contentDescription = null
                            )
                        },
                        trailing = { Switch(checked = false, onCheckedChange = null, colors = accentSwitchColors()) },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Shuffle order",
                        onClick = {},
                        subtitle = "Always randomise card order",
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.Shuffle,
                                contentDescription = null
                            )
                        },
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
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall)) {
                                FlashcardsPlayButton(
                                    onClick = {},
                                    contentDescription = "Study Compose"
                                )
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
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall)) {
                                FlashcardsPlayButton(
                                    onClick = {},
                                    contentDescription = "Study Compose Navigation"
                                )
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
                FlashcardsOverlineLabel(
                    text = "3 of 13 topics selected"
                )
                FlashcardsListGroup {
                    item(checked = true) {
                        FlashcardsSelectableListRow(
                            title = "Compose",
                            selected = true,
                            onSelectedChange = {},
                            subtitle = "80 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                    item(checked = true) {
                        FlashcardsSelectableListRow(
                            title = "Coroutines",
                            selected = true,
                            onSelectedChange = {},
                            subtitle = "40 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                    item(checked = false) {
                        FlashcardsSelectableListRow(
                            title = "Compose Navigation",
                            selected = false,
                            onSelectedChange = {},
                            subtitle = "30 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                    item(checked = false) {
                        FlashcardsSelectableListRow(
                            title = "Testing",
                            selected = false,
                            onSelectedChange = {},
                            subtitle = "25 cards",
                            trailing = { FlashcardsChevron() },
                        )
                    }
                    item(checked = true) {
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
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.NotificationsActive,
                                contentDescription = null
                            )
                        },
                        trailing = { FlashcardsChevron() },
                    )
                }
                item {
                    FlashcardsListRow(
                        title = "Microphone",
                        onClick = {},
                        subtitle = "Not granted",
                        leading = {
                            FlashcardsIconTile(
                                icon = Icons.Default.Mic,
                                contentDescription = null
                            )
                        },
                        trailing = { FlashcardsChevron() },
                    )
                }
            }
        }
    }
}
