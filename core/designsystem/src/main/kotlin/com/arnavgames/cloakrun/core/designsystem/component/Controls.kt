package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.haptics.HapticCue
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.icons.CloakIcon
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/** A selectable pill chip. Selection springs in with a soft bounce and a primary tint. */
@Composable
fun CloakChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    val haptics = CloakTheme.haptics
    val fill by animateColorAsState(
        if (selected) colors.primary.copy(alpha = 0.24f) else colors.glassTint,
        motion.snappy(),
        label = "chipFill",
    )
    val edge by animateColorAsState(if (selected) colors.primary else colors.outline, motion.snappy(), label = "chipEdge")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected && !motion.reduced) {
            pop.snapTo(0.92f)
            pop.animateTo(1f, motion.bouncy())
        }
    }
    val shape = SquircleShape.Pill
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .height(CloakTheme.sizes.minTouchTarget)
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .selectable(selected = selected, role = Role.Checkbox) {
                haptics.play(HapticCue.Tick)
                onClick()
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val tint = if (selected) colors.textPrimary else colors.textSecondary
        if (icon != null) CloakIcon(icon, contentDescription = null, tint = tint, size = 18.dp)
        Text(label, style = CloakTheme.typography.label, color = tint)
    }
}

/**
 * An on/off switch. The thumb springs across and stretches mid-travel like a drop of liquid;
 * the track glows when on.
 */
@Composable
fun CloakToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    val haptics = CloakTheme.haptics
    val position by animateFloatAsState(if (checked) 1f else 0f, motion.bouncy(), label = "toggle")
    val track by animateColorAsState(
        if (checked) colors.primaryContainer else colors.glassTintStrong,
        motion.snappy(),
        label = "track",
    )
    val trackWidth = 56.dp
    val trackHeight = 34.dp
    val thumb = 26.dp
    val inset = 4.dp
    val travel = trackWidth - thumb - inset * 2
    // Stretch peaks halfway through the travel.
    val stretch = (sin(position.coerceIn(0f, 1f) * PI).toFloat() * 10).dp
    val glow = if (checked) colors.primaryGlow else Color.Transparent
    Box(
        modifier = modifier
            .size(width = trackWidth, height = CloakTheme.sizes.minTouchTarget)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(value = checked, role = Role.Switch) {
                haptics.play(HapticCue.Tick)
                onCheckedChange(it)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(trackWidth, trackHeight)
                .dropShadow(SquircleShape.Pill, Shadow(radius = 14.dp, color = glow))
                .clip(SquircleShape.Pill)
                .background(track)
                .border(1.dp, colors.outline, SquircleShape.Pill),
        )
        Box(
            Modifier
                .offset { IntOffset((inset + travel * position - stretch * position).roundToPx(), 0) }
                .size(width = thumb + stretch, height = thumb)
                .clip(SquircleShape.Pill)
                .background(if (checked) Color.White else colors.textSecondary),
        )
    }
}

/** Segmented tabs with a glass indicator that slides under the selected option. */
@Composable
fun SegmentedTabs(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    val haptics = CloakTheme.haptics
    GlassSurface(
        shape = SquircleShape.Pill,
        modifier = modifier.height(CloakTheme.sizes.buttonHeight),
        style = GlassDefaults.chrome(),
        elevated = false,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(4.dp)) {
            val segment = maxWidth / options.size
            val indicatorX by animateDpAsState(segment * selectedIndex, motion.bouncy(), label = "indicator")
            Box(
                Modifier
                    .offset { IntOffset(indicatorX.roundToPx(), 0) }
                    .width(segment)
                    .fillMaxHeight()
                    .clip(SquircleShape.Pill)
                    .background(colors.primaryContainer),
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                options.forEachIndexed { index, option ->
                    val selected = index == selectedIndex
                    val textColor by animateColorAsState(
                        if (selected) colors.onPrimary else colors.textSecondary,
                        motion.snappy(),
                        label = "tabText",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(SquircleShape.Pill)
                            .selectable(selected = selected, role = Role.Tab) {
                                if (!selected) {
                                    haptics.play(HapticCue.Tick)
                                    onSelect(index)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option, style = CloakTheme.typography.label, color = textColor)
                    }
                }
            }
        }
    }
}

/**
 * A continuous slider from 0 to 1 (volume, sensitivity). Drag or tap anywhere on the track.
 * TalkBack can set it directly; [steps] > 0 snaps to that many intervals.
 */
@Composable
fun CloakSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val colors = CloakTheme.colors
    val density = LocalDensity.current
    val motion = CloakTheme.motion
    val scope = rememberCoroutineScope()
    val grab = remember { Animatable(1f) }
    fun snap(v: Float): Float = if (steps > 0) (v * steps).roundToInt() / steps.toFloat() else v.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(CloakTheme.sizes.minTouchTarget)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f, steps)
                setProgress { target ->
                    onValueChange(snap(target))
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val thumb = 24.dp
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(SquircleShape.Pill)
                .background(colors.glassTintStrong),
        )
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(SquircleShape.Pill)
                .background(colors.accent),
        )
        Box(
            Modifier
                .offset(x = (maxWidth - thumb) * value.coerceIn(0f, 1f))
                .size(thumb)
                .graphicsLayer {
                    scaleX = grab.value
                    scaleY = grab.value
                }
                .dropShadow(SquircleShape.Pill, Shadow(radius = 10.dp, color = colors.accent.copy(alpha = 0.5f)))
                .clip(SquircleShape.Pill)
                .background(Color.White),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .pointerInput(widthPx, steps) {
                    detectTapGestures { onValueChange(snap(it.x / widthPx)) }
                }
                .pointerInput(widthPx, steps) {
                    detectHorizontalDragGestures(
                        onDragStart = { scope.launch { grab.animateTo(1.25f, motion.snappy()) } },
                        onDragEnd = { scope.launch { grab.animateTo(1f, motion.bouncy()) } },
                        onDragCancel = { scope.launch { grab.animateTo(1f, motion.bouncy()) } },
                    ) { change, _ ->
                        change.consume()
                        onValueChange(snap(change.position.x / widthPx))
                    }
                },
        )
    }
}
