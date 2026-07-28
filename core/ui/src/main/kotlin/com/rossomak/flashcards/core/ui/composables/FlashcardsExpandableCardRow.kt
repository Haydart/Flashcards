package com.rossomak.flashcards.core.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsMotion
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * An expandable flashcard row: a leading number badge, the question [title], flat study [tags],
 * and a chevron that toggles an in-place reveal of [expandedContent] (typically the answer).
 *
 * The component owns the expand animation — the chevron rotates and the reveal fades/expands
 * here — so screens only hoist [expanded] + [onExpandedChange] and never reimplement it. The
 * header announces its expanded/collapsed state to accessibility services.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlashcardsExpandableCardRow(
    index: Int,
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    expandedStateDescription: String,
    collapsedStateDescription: String,
    modifier: Modifier = Modifier,
    tags: ImmutableList<String> = persistentListOf(),
    expandedContent: @Composable (() -> Unit)? = null,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(FlashcardsMotion.DURATION_MEDIUM_MS, easing = FlashcardsMotion.EmphasizedEasing),
        label = "chevronRotation",
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = if (expanded) expandedStateDescription else collapsedStateDescription
                }
                .clickable { onExpandedChange(!expanded) }
                .padding(
                    horizontal = MaterialTheme.spacing.normal,
                    vertical = MaterialTheme.spacing.small,
                ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.sizes.numberBadge)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxsmall),
                    ) {
                        tags.forEach { tag -> FlashcardsMetadataBadge(label = tag) }
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            if (expandedContent != null) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spacing.normal,
                            vertical = MaterialTheme.spacing.small,
                        ),
                    ) {
                        expandedContent()
                    }
                }
            }
        }
    }
}

private val previewTags = persistentListOf("Views", "State")

@ShowkaseComposable(name = "Card row — expanded", group = "Cards")
@Composable
fun FlashcardsExpandableCardRowExpandedShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsExpandableCardRow(
                index = 2,
                title = "What does remember do differently from rememberSaveable?",
                expanded = true,
                onExpandedChange = {},
                expandedStateDescription = "Expanded",
                collapsedStateDescription = "Collapsed",
                tags = persistentListOf("State"),
                expandedContent = {
                    Text(
                        text = "remember retains a value across recompositions only, while " +
                            "rememberSaveable also survives configuration changes and process death.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
            )
        }
    }
}

@ShowkaseComposable(name = "Card row — collapsed", group = "Cards")
@Composable
fun FlashcardsExpandableCardRowCollapsedShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsExpandableCardRow(
                index = 1,
                title = "What is recomposition in Jetpack Compose?",
                expanded = false,
                onExpandedChange = {},
                expandedStateDescription = "Expanded",
                collapsedStateDescription = "Collapsed",
                tags = previewTags,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsExpandableCardRowPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsExpandableCardRow(
                index = 1,
                title = "What is recomposition in Jetpack Compose?",
                expanded = false,
                onExpandedChange = {},
                expandedStateDescription = "Expanded",
                collapsedStateDescription = "Collapsed",
                tags = previewTags,
            )
        }
    }
}
