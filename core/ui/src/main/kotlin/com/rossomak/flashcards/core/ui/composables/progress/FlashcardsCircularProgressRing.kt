package com.rossomak.flashcards.core.ui.composables.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentStyle
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.spacing

private const val CIRCLE_START_ANGLE = -90f
private const val CIRCLE_SWEEP_ANGLE = 360f

/**
 * The circular determinate ring — track + progress arc, no baked-in text. Unlike
 * [FlashcardsLinearProgressBar]/[FlashcardsSegmentedProgressBar], this exposes a [content] slot:
 * every real use case centers a percentage or count inside the ring, so callers would otherwise
 * all hand-roll the same `Box(contentAlignment = Alignment.Center)` wrapper. See ADR-0035.
 */
@Composable
fun FlashcardsCircularProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: FlashcardsComponentSize = FlashcardsComponentSize.Normal,
    style: FlashcardsComponentStyle = FlashcardsComponentStyle.OnSurface,
    animate: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val clampedProgress = clampProgressBarProgress(progress)
    // Not `by` — kept as a State<Float> and dereferenced only inside the Canvas draw lambda
    // below, so a running animation re-triggers just that draw, not composition. See ADR-0035.
    val animatedProgress = animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = if (animate) flashcardsProgressBarDefaultAnimationSpec else snap(),
        label = "FlashcardsCircularProgressRingProgress",
    )
    val metrics = size.circularProgressRingMetrics()
    val fillColor = progressBarFillColorFor(style)
    val trackColor = progressBarTrackColorFor(style)

    Box(
        modifier = modifier
            .size(metrics.diameter)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clampedProgress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidthPx = metrics.strokeWidth.toPx()
            val diameterPx = metrics.diameter.toPx()
            val arcDiameter = diameterPx - strokeWidthPx
            val topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)
            val arcSize = Size(arcDiameter, arcDiameter)

            drawArc(
                color = trackColor,
                startAngle = CIRCLE_START_ANGLE,
                sweepAngle = CIRCLE_SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
            )
            // Guarded: a zero-sweep stroked arc with a round cap still renders a full-diameter dot
            // (the round cap has no zero-length path to taper), so an unguarded draw would show a
            // phantom dot at 0% progress instead of nothing.
            if (animatedProgress.value > 0f) {
                drawArc(
                    color = fillColor,
                    startAngle = CIRCLE_START_ANGLE,
                    sweepAngle = CIRCLE_SWEEP_ANGLE * animatedProgress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@ShowkaseComposable(name = "Circular — normal", group = "Progress")
@Composable
fun FlashcardsCircularProgressRingShowcase() {
    FlashcardsTheme {
        Surface {
            Box(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                FlashcardsCircularProgressRing(progress = 0.8f) {
                    Text(text = "80%", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@ShowkaseComposable(name = "Circular — small", group = "Progress")
@Composable
fun FlashcardsCircularProgressRingSmallShowcase() {
    FlashcardsTheme {
        Surface {
            Box(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                FlashcardsCircularProgressRing(progress = 0.76f, size = FlashcardsComponentSize.Small) {
                    Text(text = "76%", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@ShowkaseComposable(name = "Circular — on gradient", group = "Progress")
@Composable
fun FlashcardsCircularProgressRingOnGradientShowcase() {
    FlashcardsTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.brandColors.ctaButtonGradient)
                .padding(MaterialTheme.spacing.small),
        ) {
            FlashcardsCircularProgressRing(progress = 0.8f, style = FlashcardsComponentStyle.OnGradient) {
                Text(text = "80%", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsCircularProgressRingPreview() {
    FlashcardsTheme {
        Surface {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                FlashcardsCircularProgressRing(progress = 0.8f) {
                    Text(text = "80%", style = MaterialTheme.typography.labelLarge)
                }
                FlashcardsCircularProgressRing(progress = 0.3f, size = FlashcardsComponentSize.Small) {
                    Text(text = "30%", style = MaterialTheme.typography.labelSmall)
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.brandColors.ctaButtonGradient)
                        .padding(MaterialTheme.spacing.small),
                ) {
                    FlashcardsCircularProgressRing(progress = 0.8f, style = FlashcardsComponentStyle.OnGradient) {
                        Text(text = "80%", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
        }
    }
}
