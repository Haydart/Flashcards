package com.rossomak.flashcards.core.ui.composables.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.R
import com.rossomak.flashcards.core.ui.composables.FlashcardsDifficultyRangePill
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsTonalButton
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * One adjustable setting in a settings sheet: `label … value [Edit]`, one row among several
 * stacked with plain spacing between them (no divider — see below). Deliberately not built on
 * [FlashcardsListRow]: that component is sized (64dp min height, `titleMedium` title) for a
 * full, independently-navigable list row, and a settings sheet packs many of these into a
 * height-capped [com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheet] where every
 * extra dp of row height is a row the sheet can't show without scrolling. This row is its own,
 * intrinsically-sized `Row` instead, typeset at the same `titleSmall`/`labelMedium` pairing
 * [FlashcardsDetailedListRow] and [FlashcardsSelectableListRow] already use for their own
 * title/count text — the compact scale a details/settings-style row reads at elsewhere in the
 * app (e.g. Category Details' topic rows), rather than the larger scale a primary navigation
 * row's title needs.
 *
 * **The Edit button is the tap target, not the row.** This deliberately departs from
 * (docs/adr/0030)'s "whole row is the tap target" framing: [onClick] is wired to a real, fully
 * interactive [FlashcardsTonalButton] — not a decorative hint — so it is the row's one and only
 * clickable element, with its own accessible label and click semantics. Tapping the row outside
 * the button does nothing.
 *
 * **[label] and Edit are always fully visible; [value] is what gives way.** [label] carries no
 * `weight` at all — it lays out at its own natural (possibly two-line) width so a setting's name
 * is never the thing that gets cut, and Edit is a fixed-size sibling measured the same way. [value]
 * is the row's *only* weighted child, wrapped in a trailing-aligned [Box] that absorbs whatever
 * space [label] and Edit don't claim and ellipsizes past that. This is a deliberate reversal of an
 * earlier version where [label] carried the weight instead: two weighted siblings sharing a row was
 * tried before that and was the actual bug behind Edit visibly drifting left/right per row — a
 * `fill = false` weighted child is *placed* at its shrunk, natural width, not its allotted share, so
 * whatever came after it (Edit) inherited that same drift. Exactly one weighted child avoids that
 * regardless of which slot carries it; here it's [value], since a long `valueText` (e.g. a voice's
 * name and locale) is what's expected to run long, not [label].
 *

 * No [androidx.compose.material3.HorizontalDivider] beneath the row (unlike the row this replaced
 * — matching the design reference, which separates settings with space, not rules): the caller
 * stacks rows with its own `Arrangement.spacedBy`.
 *
 * [value] is a composable slot, not a `String`, because not every setting's current value is text:
 * a Filters row needs to render a tag count next to [FlashcardsDifficultyRangePill], which a
 * `String` can't carry. Pass [valueText] instead for the common plain-text case and leave [value]
 * at its default — the call site stays a one-liner. Supplying [value] directly overrides
 * [valueText] entirely.
 *
 * @param label What the setting is (e.g. "Mode", "Filters").
 * @param onClick Opens the setting's editor. Fires from the Edit button only.
 * @param valueText The setting's current value as plain text, rendered muted by the default
 *   [value]. Ignored once [value] is overridden.
 * @param value The setting's current value. Defaults to muted [valueText] text; override for
 *   non-text values.
 */
@Composable
fun FlashcardsSettingRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    value: @Composable () -> Unit = {
        if (valueText != null) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    },
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            value()
        }
        FlashcardsTonalButton(
            text = stringResource(R.string.common_edit_button),
            onClick = onClick,
            size = FlashcardsComponentSize.Small,
            icon = Icons.Default.Edit,
        )
    }
}

@ShowkaseComposable(name = "Setting row", group = "Lists")
@Composable
fun FlashcardsSettingRowShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsSettingRow(label = "Mode", valueText = "Rated", onClick = {})
        }
    }
}

@ShowkaseComposable(name = "Setting row — custom value", group = "Lists")
@Composable
fun FlashcardsSettingRowCustomValueShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsSettingRow(label = "Filters", onClick = {}, value = { FiltersSampleValue() })
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsSettingRowPreview() {
    FlashcardsTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                FlashcardsSettingRow(label = "Mode", valueText = "Rated", onClick = {})
                FlashcardsSettingRow(label = "Length", valueText = "18 cards", onClick = {})
                FlashcardsSettingRow(
                    label = "Voice playback settings",
                    valueText = "English (United States) · 1.25x",
                    onClick = {},
                )
                FlashcardsSettingRow(label = "Filters", onClick = {}, value = { FiltersSampleValue() })
            }
        }
    }
}

/** Sample non-text [FlashcardsSettingRow.value]: a tag count next to [FlashcardsDifficultyRangePill]. */
@Composable
private fun FiltersSampleValue() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
    ) {
        Text(
            text = "2 tags",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlashcardsDifficultyRangePill(lowLevel = 2, highLevel = 4)
    }
}
