package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.R
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.sizes
import kotlin.math.roundToInt

private const val FULL_CIRCLE_DEGREES = 360f
private const val ARC_START_DEGREES = -90f
private const val PERCENT_SCALE = 100
private const val TRACK_ALPHA = 0.16f

/**
 * Circular mastery indicator: a muted full-circle track with the filled portion drawn clockwise
 * from twelve o'clock, and the rounded percentage in the middle.
 *
 * [progress] is a fraction in `0f..1f` and is coerced into that range rather than validated — a
 * ring is decoration on a list row and must never be the thing that crashes it. The percentage is
 * announced through a single [contentDescription] on the whole component, so screen readers get
 * one meaningful phrase instead of a bare number next to an undescribed drawing.
 */
@Composable
fun FlashcardsProgressRing(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val fraction = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = TRACK_ALPHA)
    val arcColor = MaterialTheme.colorScheme.primary
    val strokeWidth = MaterialTheme.sizes.progressRingStroke

    Box(
        modifier = modifier
            .size(MaterialTheme.sizes.progressRing)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(MaterialTheme.sizes.progressRing)) {
            val stroke = Stroke(width = strokeWidth.toPx())
            // Inset by half the stroke so the ring is drawn inside its bounds rather than clipped.
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(
                color = trackColor,
                startAngle = ARC_START_DEGREES,
                sweepAngle = FULL_CIRCLE_DEGREES,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = arcColor,
                startAngle = ARC_START_DEGREES,
                sweepAngle = FULL_CIRCLE_DEGREES * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
        Text(
            // Resource rather than string interpolation: the percent sign's placement and spacing
            // are locale-dependent (fr-FR renders "58 %"), and `%1$d` formats the digits for the
            // current locale instead of always emitting ASCII.
            text = stringResource(
                R.string.common_mastery_progress_label,
                (fraction * PERCENT_SCALE).roundToInt(),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@ShowkaseComposable(name = "Progress ring", group = "Lists")
@Composable
fun FlashcardsProgressRingShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsProgressRing(progress = 0.58f, contentDescription = "58% mastered")
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsProgressRingPreview() {
    FlashcardsTheme {
        Surface {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                FlashcardsProgressRing(progress = 0.86f, contentDescription = "86% mastered")
            }
        }
    }
}
