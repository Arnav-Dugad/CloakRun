package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassEnvironment
import com.arnavgames.cloakrun.core.designsystem.glass.GlassStyle
import com.arnavgames.cloakrun.core.designsystem.glass.currentGlassEnvironment
import com.arnavgames.cloakrun.core.designsystem.glass.glass
import com.arnavgames.cloakrun.core.designsystem.haptics.HapticCue
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.token.CloakColors
import com.arnavgames.cloakrun.core.icons.CloakIcon
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class CloakButtonStyle { Primary, Glass, Ghost, Danger }

/**
 * The CloakRun button. Fully rounded, at least 56dp tall, with squash and stretch on press and a
 * specular highlight that follows the finger.
 */
@Composable
fun CloakButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CloakButtonStyle = CloakButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    large: Boolean = false,
) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    val haptics = CloakTheme.haptics
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var touch by remember { mutableStateOf(Offset.Unspecified) }

    val squash by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) motion.snappy() else motion.bouncy(),
        label = "squash",
    )
    val highlight by animateFloatAsState(if (pressed) 1f else 0f, motion.snappy(), label = "highlight")

    val contentColor = when (style) {
        CloakButtonStyle.Primary -> colors.onPrimary
        CloakButtonStyle.Glass -> colors.textPrimary
        CloakButtonStyle.Ghost -> colors.accent
        CloakButtonStyle.Danger -> colors.danger
    }
    val height = if (large) CloakTheme.sizes.buttonHeightLarge else CloakTheme.sizes.buttonHeight

    val body = Modifier
        .defaultMinSize(minHeight = height, minWidth = height * 2)
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                touch = down.position
                do {
                    val event = awaitPointerEvent()
                    event.changes.firstOrNull()?.let { touch = it.position }
                } while (event.changes.any { it.pressed })
            }
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
        ) {
            haptics.play(HapticCue.Confirm)
            onClick()
        }
        .drawWithContent {
            drawContent()
            if (highlight > 0f && touch != Offset.Unspecified) {
                drawRect(
                    Brush.radialGradient(
                        0f to Color.White.copy(alpha = 0.28f * highlight),
                        1f to Color.Transparent,
                        center = touch,
                        radius = size.height * 1.4f,
                    ),
                )
            }
        }
        .padding(horizontal = CloakTheme.spacing.xl)

    val transformed = modifier
        .graphicsLayer {
            // Squash: wider and shorter while pressed; the bouncy release overshoots into a stretch.
            scaleX = 1f + 0.035f * squash
            scaleY = 1f - 0.07f * squash
        }
        .alpha(if (enabled) 1f else 0.4f)

    val label: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) CloakIcon(icon, contentDescription = null, tint = contentColor, size = 20.dp)
            Text(text = text, style = CloakTheme.typography.label, color = contentColor)
        }
    }

    val glassStyle = GlassDefaults.chrome()
    val environment = currentGlassEnvironment()
    Box(
        modifier = transformed.buttonFrame(style, enabled, colors, glassStyle, environment).then(body),
        contentAlignment = Alignment.Center,
    ) { label() }
}

/** The style-specific background, rim and glow of a [CloakButton]. */
private fun Modifier.buttonFrame(
    style: CloakButtonStyle,
    enabled: Boolean,
    colors: CloakColors,
    glassStyle: GlassStyle,
    environment: GlassEnvironment,
): Modifier {
    val shape = SquircleShape.Pill
    val frame = when (style) {
        CloakButtonStyle.Primary -> {
            val glow = if (enabled) {
                Modifier.dropShadow(shape, Shadow(radius = 22.dp, color = colors.primaryGlow, offset = DpOffset(0.dp, 8.dp)))
            } else {
                Modifier
            }
            glow
                .clip(shape)
                .background(Brush.verticalGradient(listOf(colors.primaryContainer, lerp(colors.primaryContainer, Color.Black, 0.14f))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)), shape)
        }

        CloakButtonStyle.Glass -> Modifier.glass(shape, glassStyle, environment)

        CloakButtonStyle.Ghost -> Modifier.clip(shape).border(1.dp, colors.outline, shape)

        CloakButtonStyle.Danger ->
            Modifier
                .clip(shape)
                .background(colors.danger.copy(alpha = 0.16f))
                .border(1.dp, colors.danger.copy(alpha = 0.5f), shape)
    }
    return then(frame)
}

/**
 * A destructive action that must be held for [holdMillis] to confirm. A ring around the leading
 * icon fills and a wash sweeps across the button while held; letting go early springs it back.
 * Accessibility services get a direct "confirm" action, because holding is not accessible.
 */
@Composable
fun HoldToConfirmButton(
    text: String,
    icon: ImageVector,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Int = 1_200,
) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    val haptics = CloakTheme.haptics
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    val shape = SquircleShape.Pill

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = CloakTheme.sizes.buttonHeight, minWidth = CloakTheme.sizes.buttonHeight * 3)
            .clip(shape)
            .background(colors.danger.copy(alpha = 0.14f))
            .drawWithContent {
                drawRect(colors.danger.copy(alpha = 0.32f), size = size.copy(width = size.width * progress.value))
                drawContent()
            }
            .border(1.dp, colors.danger.copy(alpha = 0.55f), shape)
            .semantics {
                role = Role.Button
                stateDescription = "${(progress.value * 100).toInt()}%"
                onClick(label = text) {
                    onConfirm()
                    true
                }
            }
            .pointerInput(holdMillis) {
                awaitEachGesture {
                    awaitFirstDown()
                    haptics.play(HapticCue.Tick)
                    job?.cancel()
                    job = scope.launch {
                        progress.animateTo(1f, tween((holdMillis * (1f - progress.value)).toInt(), easing = LinearEasing))
                        haptics.play(HapticCue.Caught)
                        onConfirm()
                        progress.snapTo(0f)
                    }
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    if (progress.value < 1f) {
                        job?.cancel()
                        scope.launch { progress.animateTo(0f, motion.bouncy()) }
                    }
                }
            }
            .padding(horizontal = CloakTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(30.dp)) {
                    val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(colors.danger.copy(alpha = 0.25f), style = stroke)
                    drawArc(colors.danger, -90f, 360f * progress.value, useCenter = false, style = stroke)
                }
                CloakIcon(icon, contentDescription = null, tint = colors.danger, size = 16.dp)
            }
            Text(text = text, style = CloakTheme.typography.label, color = colors.danger)
        }
    }
}
