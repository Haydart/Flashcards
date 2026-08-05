package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List as AutoMirroredList
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

private const val GRADIENT_CONTAINER_ALPHA = 0.16f
private const val GRADIENT_BORDER_ALPHA = 0.22f

/**
 * Which background a [FlashcardsMetadataBadge] is drawn on. The badge cannot read the surface
 * behind it, so the caller declares it.
 */
enum class MetadataBadgeStyle {
    /** Default — a tinted pill for use on a plain themed surface. */
    Surface,

    /**
     * Translucent pill for use on [BrandColors.topBarGradient] (top bars, hero headers). That
     * gradient never flips to a dark variant, so this style's colors are theme-independent.
     */
    OnGradient,
}

/**
 * The metadata badge — the smaller, read-only sibling of [FlashcardsTagChip]: a 32dp pill with a
 * fully rounded corner, an optional leading icon, and a compact label. Used for at-a-glance stats
 * ("~ 12 min", "3 topics", "42 cards") and for the flat study tags shown on card rows.
 *
 * Pass [onClick] only when the badge doubles as an affordance (e.g. a sort badge that opens a
 * dialog); it then reports itself to accessibility as a button.
 */
@Composable
fun FlashcardsMetadataBadge(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: MetadataBadgeStyle = MetadataBadgeStyle.Surface,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(MaterialTheme.cornerRadius.full)
    val onGradient = MaterialTheme.brandColors.onTopBarGradient
    val containerColor = when (style) {
        MetadataBadgeStyle.Surface -> MaterialTheme.colorScheme.surfaceVariant
        MetadataBadgeStyle.OnGradient -> onGradient.copy(alpha = GRADIENT_CONTAINER_ALPHA)
    }
    val contentColor = when (style) {
        MetadataBadgeStyle.Surface -> MaterialTheme.colorScheme.onSurface
        MetadataBadgeStyle.OnGradient -> onGradient
    }
    val iconColor = when (style) {
        MetadataBadgeStyle.Surface -> MaterialTheme.colorScheme.onSurfaceVariant
        MetadataBadgeStyle.OnGradient -> onGradient
    }
    val border = when (style) {
        MetadataBadgeStyle.Surface -> null
        MetadataBadgeStyle.OnGradient -> BorderStroke(
            width = MaterialTheme.sizes.hairline,
            color = onGradient.copy(alpha = GRADIENT_BORDER_ALPHA),
        )
    }

    Surface(
        modifier = modifier
            .height(MaterialTheme.sizes.metadataBadgeHeight)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxsmall),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(MaterialTheme.sizes.metadataBadgeIcon),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun MetadataBadgeSurfaceSampleRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
    ) {
        FlashcardsMetadataBadge(label = "~ 12 min", icon = Icons.Default.DateRange)
        FlashcardsMetadataBadge(label = "3 topics", icon = Icons.AutoMirrored.Filled.AutoMirroredList)
        FlashcardsMetadataBadge(label = "42 cards", icon = Icons.Default.Star)
    }
}

@ShowkaseComposable(name = "Metadata badge — on surface", group = "Chips & Badges")
@Composable
fun FlashcardsMetadataBadgeSurfaceShowcase() {
    FlashcardsTheme {
        Surface {
            MetadataBadgeSurfaceSampleRow()
        }
    }
}

@ShowkaseComposable(name = "Metadata badge — on gradient", group = "Chips & Badges")
@Composable
fun FlashcardsMetadataBadgeGradientShowcase() {
    FlashcardsTheme {
        Surface {
            MetadataBadgeGradientSampleRow()
        }
    }
}

@ShowkaseComposable(name = "Metadata badge — label only", group = "Chips & Badges")
@Composable
fun FlashcardsMetadataBadgeLabelOnlyShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsMetadataBadge(
                label = "State",
                modifier = Modifier.padding(MaterialTheme.spacing.small),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsMetadataBadgePreview() {
    FlashcardsTheme {
        Surface {
            MetadataBadgeSampleColumn()
        }
    }
}

@Composable
private fun MetadataBadgeSampleColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
    ) {
        MetadataBadgeSurfaceSampleRow()
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
        ) {
            FlashcardsMetadataBadge(label = "State")
            FlashcardsMetadataBadge(label = "Coroutines")
            FlashcardsMetadataBadge(label = "private")
        }
        MetadataBadgeGradientSampleRow()
    }
}

@Composable
private fun MetadataBadgeGradientSampleRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.brandColors.topBarGradient)
            .padding(MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
    ) {
        FlashcardsMetadataBadge(
            label = "~ 12 min",
            icon = Icons.Default.DateRange,
            style = MetadataBadgeStyle.OnGradient,
        )
        FlashcardsMetadataBadge(
            label = "3 topics",
            icon = Icons.Default.List,
            style = MetadataBadgeStyle.OnGradient,
        )
        FlashcardsMetadataBadge(
            label = "42 cards",
            icon = Icons.Default.Star,
            style = MetadataBadgeStyle.OnGradient,
        )
    }
}
