package com.arnavgames.cloakrun.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Grid size every CloakRun icon is drawn on. */
const val ICON_GRID = 24f

/** Stroke width shared by every icon, in grid units (2dp at the default 24dp size). */
const val ICON_STROKE = 2f

/**
 * A tiny DSL that keeps every icon on the same 24 x 24 grid with a 2dp stroke, round caps and
 * round joins. Icons are drawn in black and tinted at the call site.
 */
internal class IconScope {
    val strokes = mutableListOf<String>()
    val fills = mutableListOf<String>()

    /** Adds stroked SVG path data. */
    fun stroke(pathData: String) {
        strokes += pathData
    }

    /** Adds filled SVG path data (used sparingly, for dots and solid marks). */
    fun fill(pathData: String) {
        fills += pathData
    }

    fun circle(
        cx: Float,
        cy: Float,
        r: Float,
    ) = stroke(circlePath(cx, cy, r))

    fun dot(
        cx: Float,
        cy: Float,
        r: Float = 1.1f,
    ) = fill(circlePath(cx, cy, r))

    fun roundRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        r: Float,
    ) = stroke(roundRectPath(x, y, w, h, r))

    /** A dashed circle drawn as [count] arcs, each covering [duty] of its slot. */
    fun dashedCircle(
        cx: Float,
        cy: Float,
        r: Float,
        count: Int,
        duty: Float,
        rotationDeg: Float = 0f,
    ) {
        val slot = 2 * PI / count
        val rot = rotationDeg * PI / 180
        for (i in 0 until count) {
            val a0 = rot + i * slot
            val a1 = a0 + slot * duty
            stroke(arcPath(cx, cy, r, a0, a1))
        }
    }
}

internal fun icon(
    name: String,
    autoMirror: Boolean = false,
    block: IconScope.() -> Unit,
): ImageVector {
    val scope = IconScope().apply(block)
    return ImageVector.Builder(
        name = "CloakIcons.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = ICON_GRID,
        viewportHeight = ICON_GRID,
        autoMirror = autoMirror,
    ).apply {
        for (d in scope.strokes) {
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = ICON_STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        for (d in scope.fills) {
            addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
        }
    }.build()
}

private fun Float.f(): String = "%.3f".format(java.util.Locale.ROOT, this).trimEnd('0').trimEnd('.')

private fun Double.f(): String = toFloat().f()

internal fun circlePath(
    cx: Float,
    cy: Float,
    r: Float,
): String = "M${(cx - r).f()} ${cy.f()}a${r.f()} ${r.f()} 0 1 0 ${(2 * r).f()} 0a${r.f()} ${r.f()} 0 1 0 ${(-2 * r).f()} 0Z"

internal fun roundRectPath(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    r: Float,
): String = buildString {
    append("M${(x + r).f()} ${y.f()}H${(x + w - r).f()}")
    append("A${r.f()} ${r.f()} 0 0 1 ${(x + w).f()} ${(y + r).f()}V${(y + h - r).f()}")
    append("A${r.f()} ${r.f()} 0 0 1 ${(x + w - r).f()} ${(y + h).f()}H${(x + r).f()}")
    append("A${r.f()} ${r.f()} 0 0 1 ${x.f()} ${(y + h - r).f()}V${(y + r).f()}")
    append("A${r.f()} ${r.f()} 0 0 1 ${(x + r).f()} ${y.f()}Z")
}

/** Arc from angle [a0] to [a1] (radians, clockwise from 3 o'clock in screen space). */
internal fun arcPath(
    cx: Float,
    cy: Float,
    r: Float,
    a0: Double,
    a1: Double,
): String {
    val x0 = cx + r * cos(a0)
    val y0 = cy + r * sin(a0)
    val x1 = cx + r * cos(a1)
    val y1 = cy + r * sin(a1)
    val large = if (a1 - a0 > PI) 1 else 0
    return "M${x0.f()} ${y0.f()}A${r.f()} ${r.f()} 0 $large 1 ${x1.f()} ${y1.f()}"
}
