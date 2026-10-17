package com.arnavgames.cloakrun.core.map.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.token.CloakMotion
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.map.CloakMapState

/** A looping 0..1 phase, frozen when motion is reduced. */
@Composable
internal fun rememberLoop(
    periodMs: Int,
    reduced: Boolean,
    frozenAt: Float = 0.35f,
): State<Float> {
    if (reduced) return remember { mutableFloatStateOf(frozenAt) }
    return rememberInfiniteTransition(label = "loop").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
}

/**
 * The player's own position: an accuracy halo sized to the real GPS accuracy, and a calm core
 * that breathes. Hidden until the first fix.
 */
@Composable
fun PlayerDotOverlay(
    state: CloakMapState,
    fix: LocationFix?,
    modifier: Modifier = Modifier,
    color: Color = CloakTheme.colors.accent,
) {
    val breath = rememberLoop(CloakMotion.BREATH_PERIOD_MS, CloakTheme.motion.reduced)
    Canvas(modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        state.frame // Redraw on every map frame.
        val f = fix ?: return@Canvas
        val center = state.toScreen(f.point) ?: return@Canvas
        val halo = (f.accuracyM / state.metersPerPixel(f.point.lat)).toFloat().coerceIn(12.dp.toPx(), size.maxDimension)
        drawCircle(color.copy(alpha = 0.10f), halo, center)
        drawCircle(color.copy(alpha = 0.30f), halo, center, style = Stroke(1.dp.toPx()))
        val b = breath.value
        val pulse = 9.dp.toPx() + 10.dp.toPx() * b
        drawCircle(color.copy(alpha = 0.35f * (1f - b)), pulse, center)
        drawCircle(Color.White, 8.dp.toPx(), center)
        drawCircle(color, 6.dp.toPx(), center)
    }
}

/**
 * The play area: the world outside is dimmed, and the border is a glowing, animated, dashed
 * energy line. While [editing], each polygon vertex shows a handle.
 */
@Composable
fun PlayAreaOverlay(
    state: CloakMapState,
    area: PlayArea?,
    modifier: Modifier = Modifier,
    valid: Boolean = true,
    editing: Boolean = false,
) {
    val colors = CloakTheme.colors
    val flow = rememberLoop(CloakMotion.SHIMMER_PERIOD_MS, CloakTheme.motion.reduced)
    val edge = if (valid) colors.accent else colors.danger
    Canvas(modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        state.frame
        val a = area ?: return@Canvas
        val points = a.outline().mapNotNull(state::toScreen)
        if (points.size < 3) return@Canvas
        val ring = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            close()
        }
        drawOutside(ring, colors.scrim.copy(alpha = 0.42f))
        drawEnergyLine(ring, edge, flow.value)
        if (editing && a is PlayArea.Polygon) {
            points.forEach { p -> drawHandle(p, edge) }
        }
    }
}

private fun DrawScope.drawOutside(ring: Path, scrim: Color) {
    val outside = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
        addPath(ring)
    }
    drawPath(outside, scrim)
}

private fun DrawScope.drawEnergyLine(
    ring: Path,
    color: Color,
    phase: Float,
) {
    val dash = 14.dp.toPx()
    val gap = 9.dp.toPx()
    drawPath(ring, color.copy(alpha = 0.14f), style = Stroke(width = 16.dp.toPx()))
    drawPath(ring, color.copy(alpha = 0.22f), style = Stroke(width = 7.dp.toPx()))
    drawPath(
        ring,
        Brush.linearGradient(listOf(color, Color.White.copy(alpha = 0.9f), color)),
        style = Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), phase = -phase * (dash + gap) * 2),
        ),
    )
}

private fun DrawScope.drawHandle(p: Offset, color: Color) {
    drawCircle(color.copy(alpha = 0.25f), 16.dp.toPx(), p)
    drawCircle(Color.White, 8.dp.toPx(), p)
    drawCircle(color, 5.dp.toPx(), p)
}
