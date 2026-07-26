// Grouping file: the FlashcardsListGroup composable is the API; FlashcardsListGroupScope is
// its supporting DSL receiver.
@file:Suppress("MatchingDeclarationName")

package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.hairlineBorder

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

@ShowkaseComposable(name = "List group", group = "Lists")
@Composable
fun FlashcardsListGroupShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsListGroup {
                item {
                    FlashcardsListRow(title = "Notifications", onClick = {}, subtitle = "Allowed")
                }
                item {
                    FlashcardsListRow(title = "Microphone", onClick = {}, subtitle = "Not granted")
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
                    FlashcardsListRow(title = "Notifications", onClick = {}, subtitle = "Allowed")
                }
                item {
                    FlashcardsListRow(title = "Microphone", onClick = {}, subtitle = "Not granted")
                }
            }
        }
    }
}
