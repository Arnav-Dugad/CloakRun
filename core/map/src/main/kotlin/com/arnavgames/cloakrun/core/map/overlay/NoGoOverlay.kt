package com.arnavgames.cloakrun.core.map.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.safety.Hazard
import com.arnavgames.cloakrun.core.game.safety.HazardKind
import com.arnavgames.cloakrun.core.map.CloakMapState

/**
 * No-go zones inside the play area: water and restricted land are hatched, roads and rail are
 * drawn as danger corridors at their true width, and buildings get a faint tint. Everything is
 * clipped to the play area, so the outside world stays clean.
 */
@Composable
fun NoGoOverlay(
    state: CloakMapState,
    area: PlayArea?,
    hazards: List<Hazard>,
    modifier: Modifier = Modifier,
) {
    val danger = CloakTheme.colors.danger
    val warning = CloakTheme.colors.warning
    Canvas(modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        state.frame
        val a = area ?: return@Canvas
        if (hazards.isEmpty()) return@Canvas
        val clip = projectedPath(state, a.outline(), close = true) ?: return@Canvas
        val metersPerPx = state.metersPerPixel(a.center.lat).toFloat().coerceAtLeast(0.01f)
        clipPath(clip) {
            for (h in hazards) {
                when (h) {
                    is Hazard.Area -> drawArea(state, h, if (h.kind == HazardKind.Restricted) warning else danger)
                    is Hazard.Corridor -> drawCorridor(state, h, danger, metersPerPx)
                }
            }
        }
    }
}

private fun DrawScope.drawArea(
    state: CloakMapState,
    hazard: Hazard.Area,
    color: Color,
) {
    val path = projectedPath(state, hazard.ring, close = true) ?: return
    if (hazard.kind == HazardKind.Building) {
        drawPath(path, color.copy(alpha = 0.10f))
        return
    }
    drawPath(path, color.copy(alpha = 0.16f))
    clipPath(path) { drawHatch(color.copy(alpha = 0.45f)) }
    drawPath(path, color.copy(alpha = 0.7f), style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawCorridor(
    state: CloakMapState,
    hazard: Hazard.Corridor,
    color: Color,
    metersPerPx: Float,
) {
    val path = projectedPath(state, hazard.line, close = false) ?: return
    val width = (hazard.halfWidthM * 2 / metersPerPx).toFloat().coerceAtLeast(4.dp.toPx())
    drawPath(path, color.copy(alpha = 0.22f), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(
        path,
        color.copy(alpha = 0.8f),
        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
    )
}

/** Diagonal hatch lines across the whole canvas; callers clip it to a shape. */
private fun DrawScope.drawHatch(color: Color) {
    val gap = 9.dp.toPx()
    val stroke = 1.2.dp.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stroke)
        x += gap
    }
}

private fun projectedPath(
    state: CloakMapState,
    points: List<com.arnavgames.cloakrun.core.game.geo.GeoPoint>,
    close: Boolean,
): Path? {
    val xy = state.toScreen(points) ?: return null
    if (xy.size < 4) return null
    return Path().apply {
        moveTo(xy[0], xy[1])
        var i = 2
        while (i < xy.size) {
            lineTo(xy[i], xy[i + 1])
            i += 2
        }
        if (close) close()
    }
}
