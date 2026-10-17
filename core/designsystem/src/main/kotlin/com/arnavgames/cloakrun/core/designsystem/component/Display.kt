package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.token.CloakMotion
import com.arnavgames.cloakrun.core.icons.CloakIcon
import kotlinx.coroutines.delay

/** A glass card with the 28dp continuous corner. Clickable when [onClick] is set. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = SquircleShape(CloakTheme.radii.card)
    GlassSurface(shape = shape, modifier = modifier, style = GlassDefaults.surface()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(CloakTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s),
            content = content,
        )
    }
}

/**
 * A rolling counter: each character rolls up (or down) independently with a spring when it
 * changes, so only the digits that change move. Uses tabular figures so the width never jitters.
 */
@Composable
fun NumberTicker(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = CloakTheme.typography.numeric,
    color: Color = CloakTheme.colors.textPrimary,
    countingUp: Boolean = true,
) {
    val motion = CloakTheme.motion
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = text }) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (motion.reduced) {
                        ContentTransform(fadeIn(tween(0)), fadeOut(tween(0)))
                    } else {
                        val dir = if (countingUp) 1 else -1
                        (
                            slideInVertically(motion.bouncy<IntOffset>()) { it * dir } + fadeIn(motion.snappy<Float>())
                            ) togetherWith (slideOutVertically(motion.smooth<IntOffset>()) { -it * dir } + fadeOut(motion.snappy<Float>()))
                    }
                },
                label = "ticker$index",
            ) { c ->
                Text(c.toString(), style = style, color = color, modifier = Modifier.clearAndSetSemantics { })
            }
        }
    }
}

/**
 * Split-flap text, like a departure board. When [text] changes, each tile flips over to its new
 * character, staggered left to right. Used for join codes.
 */
@Composable
fun SplitFlapText(
    text: String,
    modifier: Modifier = Modifier,
    tileWidth: Dp = 44.dp,
    contentDescription: String = text,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        text.forEachIndexed { index, char -> FlapTile(char, index, tileWidth) }
    }
}

@Composable
private fun FlapTile(
    char: Char,
    index: Int,
    width: Dp,
) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    var shown by remember { mutableStateOf(char) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(char) {
        if (char == shown) return@LaunchedEffect
        if (motion.reduced) {
            shown = char
            return@LaunchedEffect
        }
        delay(index * FLAP_STAGGER_MS)
        rotation.animateTo(90f, tween(FLAP_HALF_MS))
        shown = char
        rotation.snapTo(-90f)
        rotation.animateTo(0f, motion.bouncy())
    }
    val shape = SquircleShape(12.dp)
    Box(
        modifier = Modifier
            .size(width = width, height = width * 1.3f)
            .graphicsLayer {
                rotationX = rotation.value
                cameraDistance = 12f * density
            }
            .clip(shape)
            .background(
                // White glass disappears on light cards, so Daylight tiles use a faint ink tint instead.
                if (colors.isDark) {
                    Brush.verticalGradient(listOf(colors.glassTintStrong, colors.glassTint))
                } else {
                    Brush.verticalGradient(listOf(colors.textPrimary.copy(alpha = 0.10f), colors.textPrimary.copy(alpha = 0.05f)))
                },
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(shown.toString(), style = CloakTheme.typography.headline, color = colors.textPrimary)
        Box(
            Modifier
                .fillMaxWidth()
                .size(width = width, height = 1.dp)
                .background(colors.background.copy(alpha = 0.6f)),
        )
    }
}

private const val FLAP_STAGGER_MS = 70L
private const val FLAP_HALF_MS = 110

/** Loading indicator in the CloakRun language: a radar sweep with expanding rings. */
@Composable
fun RadarLoader(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = CloakTheme.colors.accent,
) {
    val reduced = CloakTheme.motion.reduced
    val angle by if (reduced) {
        remember { mutableFloatStateOf(300f) }
    } else {
        rememberInfiniteTransition(label = "radar").animateFloat(
            0f,
            360f,
            infiniteRepeatable(tween(CloakMotion.SWEEP_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "sweep",
        )
    }
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        val ring = Stroke(1.5.dp.toPx())
        drawCircle(color.copy(alpha = 0.25f), r - 1.dp.toPx(), style = ring)
        drawCircle(color.copy(alpha = 0.18f), r * 0.62f, style = ring)
        drawCircle(color.copy(alpha = 0.12f), r * 0.28f, style = ring)
        rotate(angle) {
            drawCircle(
                Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.78f to Color.Transparent,
                    1f to color.copy(alpha = 0.55f),
                    center = center,
                ),
                r,
            )
            drawLine(color, center, Offset(center.x + r, center.y), strokeWidth = 2.dp.toPx())
        }
        drawCircle(color, 3.dp.toPx())
    }
}

/** Empty and error states share one layout: an animated illustration, a title, a body and an action. */
@Composable
fun StateMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = CloakTheme.colors
    val tint = if (isError) colors.danger else colors.accent
    Column(
        modifier = modifier.fillMaxWidth().padding(CloakTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m),
    ) {
        Box(contentAlignment = Alignment.Center) {
            RadarLoader(size = 120.dp, color = tint)
            CloakIcon(icon, contentDescription = null, tint = tint, size = 32.dp)
        }
        Text(
            title,
            style = CloakTheme.typography.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(body, style = CloakTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center)
        action?.invoke()
    }
}
