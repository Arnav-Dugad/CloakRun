package com.arnavgames.cloakrun.core.designsystem.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * A rounded rectangle with continuous (superellipse) corners, the curvature-continuous corner
 * style that reads as softer and more physical than a circular arc.
 *
 * Each corner is a quadrant of the superellipse |x|^n + |y|^n = 1 with [exponent] n (about 5),
 * spread over `radius * (1 + smoothing)` so that it reads at the same visual size as a circular
 * corner of [radius] while blending into the straight edges without a curvature jump.
 *
 * When the requested corner does not fit (for example a pill, where [radius] is [Dp.Infinity]),
 * the corner extent is clamped to half the shorter side and the exponent eases toward 2, so a
 * fully rounded control becomes a true capsule instead of a squarish lozenge.
 */
@Immutable
class SquircleShape(
    private val radius: Dp,
    private val smoothing: Float = DEFAULT_SMOOTHING,
    private val exponent: Float = DEFAULT_EXPONENT,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Rectangle(size.toRect())
        val half = min(size.width, size.height) / 2f
        val requested = if (radius == Dp.Infinity) Float.POSITIVE_INFINITY else with(density) { radius.toPx() }
        if (requested <= 0f) return Outline.Rectangle(size.toRect())
        val desiredExtent = requested * (1f + smoothing)
        val extent = min(desiredExtent, half)
        val fit = if (desiredExtent.isInfinite()) 0f else (extent / desiredExtent).coerceIn(0f, 1f)
        val n = CAPSULE_EXPONENT + (exponent - CAPSULE_EXPONENT) * fit
        return Outline.Generic(squirclePath(size, extent, n))
    }

    override fun equals(other: Any?): Boolean = other is SquircleShape &&
        other.radius == radius &&
        other.smoothing == smoothing &&
        other.exponent == exponent

    override fun hashCode(): Int = (radius.hashCode() * 31 + smoothing.hashCode()) * 31 + exponent.hashCode()

    override fun toString(): String = "SquircleShape(radius=$radius, smoothing=$smoothing, exponent=$exponent)"

    companion object {
        const val DEFAULT_SMOOTHING = 0.6f
        const val DEFAULT_EXPONENT = 5f
        private const val CAPSULE_EXPONENT = 2f
        private const val SEGMENTS_PER_CORNER = 24

        /** Fully rounded ends: buttons, chips, pills. */
        val Pill = SquircleShape(Dp.Infinity)

        /** Builds the squircle outline. Exposed for tests and for drawing glass rims along the same path. */
        fun squirclePath(
            size: Size,
            extent: Float,
            n: Float,
        ): Path = Path().apply {
            val w = size.width
            val h = size.height
            val e = extent
            val p = 2f / n

            // Corner sample i in 0..SEGMENTS maps theta from 0 to 90 degrees.
            fun cx(i: Int) = cos(i * (PI / 2) / SEGMENTS_PER_CORNER).toFloat().coerceAtLeast(0f).pow(p)
            fun sy(i: Int) = sin(i * (PI / 2) / SEGMENTS_PER_CORNER).toFloat().coerceAtLeast(0f).pow(p)

            // Start at the end of the top-left corner on the top edge, go clockwise.
            moveTo(e, 0f)
            lineTo(w - e, 0f)
            for (i in 0..SEGMENTS_PER_CORNER) { // top-right: top edge -> right edge
                lineTo(w - e + e * sy(i), e - e * cx(i))
            }
            lineTo(w, h - e)
            for (i in 0..SEGMENTS_PER_CORNER) { // bottom-right: right edge -> bottom edge
                lineTo(w - e + e * cx(i), h - e + e * sy(i))
            }
            lineTo(e, h)
            for (i in 0..SEGMENTS_PER_CORNER) { // bottom-left: bottom edge -> left edge
                lineTo(e - e * sy(i), h - e + e * cx(i))
            }
            lineTo(0f, e)
            for (i in 0..SEGMENTS_PER_CORNER) { // top-left: left edge -> top edge
                lineTo(e - e * cx(i), e - e * sy(i))
            }
            close()
        }
    }
}

private fun Size.toRect() = androidx.compose.ui.geometry.Rect(0f, 0f, width, height)

/** Convenience for the design tokens: `SquircleShape(CloakTheme.radii.card)`. */
fun squircle(radius: Dp = 28.dp): SquircleShape = SquircleShape(radius)
