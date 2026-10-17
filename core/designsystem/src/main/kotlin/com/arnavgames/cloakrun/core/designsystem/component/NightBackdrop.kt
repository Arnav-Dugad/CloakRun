package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.token.CloakMotion
import kotlin.math.max

/**
 * The CloakRun night backdrop: midnight base, a violet aura and slowly expanding radar rings
 * centred on [focus] (fractions of the size). The rings stop when motion is reduced.
 */
@Composable
fun NightBackdrop(
    modifier: Modifier = Modifier,
    focus: Offset = Offset(0.5f, 0.32f),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = CloakTheme.colors
    val reduced = CloakTheme.motion.reduced
    val phase = radarPhase(reduced)
    Box(modifier.fillMaxSize().background(colors.background)) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * focus.x, size.height * focus.y)
            val reach = max(size.width, size.height)
            drawRect(
                Brush.radialGradient(
                    0f to colors.backgroundGlow,
                    0.55f to colors.backgroundGlow.copy(alpha = colors.backgroundGlow.alpha * 0.25f),
                    1f to colors.backgroundGlow.copy(alpha = 0f),
                    center = center,
                    radius = reach * 0.75f,
                ),
            )
            val ringStroke = Stroke(width = 1.5.dp.toPx())
            for (i in 0 until RING_COUNT) {
                val t = ((phase.value + i.toFloat() / RING_COUNT) % 1f)
                val radius = reach * (0.12f + 0.78f * t)
                val alpha = (1f - t) * 0.16f
                drawCircle(colors.textPrimary.copy(alpha = alpha), radius, center, style = ringStroke)
            }
        }
        content()
    }
}

private const val RING_COUNT = 3

@Composable
private fun radarPhase(reduced: Boolean): State<Float> {
    if (reduced) return remember { StaticPhase }
    val transition = rememberInfiniteTransition(label = "radar")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CloakMotion.SWEEP_PERIOD_MS * 3, easing = LinearEasing), RepeatMode.Restart),
        label = "radarPhase",
    )
}

private object StaticPhase : State<Float> {
    override val value: Float = 0.2f
}
