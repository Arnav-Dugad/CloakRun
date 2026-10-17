package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Resting positions of a [GlassSheetLayout]. */
enum class SheetDetent { Peek, Half, Full }

@Stable
class GlassSheetState(initial: SheetDetent) {
    internal val draggable = AnchoredDraggableState(initial)

    /** The detent the sheet is resting at, or heading to after a fling. */
    val detent: SheetDetent get() = draggable.targetValue

    internal suspend fun settleTo(detent: SheetDetent, motion: com.arnavgames.cloakrun.core.designsystem.token.CloakMotion) {
        draggable.animateTo(detent, motion.smooth())
    }
}

@Composable
fun rememberGlassSheetState(initial: SheetDetent = SheetDetent.Peek): GlassSheetState = remember { GlassSheetState(initial) }

/**
 * A screen with a detented glass bottom sheet (Peek, Half, Full). The sheet's corner radius,
 * blur strength and the scrim behind it follow drag progress; dragging past the ends stretches
 * with the platform overscroll. TalkBack gets Expand and Collapse actions.
 */
@Composable
fun GlassSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    expandLabel: String,
    collapseLabel: String,
    modifier: Modifier = Modifier,
    state: GlassSheetState = rememberGlassSheetState(),
    peekHeight: Dp = 132.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val motion = CloakTheme.motion
    val colors = CloakTheme.colors
    val radii = CloakTheme.radii
    val scope = rememberCoroutineScope()
    val overscroll = rememberOverscrollEffect()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val fullHeight = with(density) { maxHeight.toPx() }
        val fullTop = fullHeight * FULL_TOP_FRACTION
        val halfTop = fullHeight * HALF_TOP_FRACTION
        val peekTop = fullHeight - with(density) { peekHeight.toPx() }
        SideEffect {
            state.draggable.updateAnchors(
                DraggableAnchors {
                    SheetDetent.Full at fullTop
                    SheetDetent.Half at halfTop
                    SheetDetent.Peek at peekTop
                },
            )
        }
        val progress by remember(fullTop, peekTop) {
            derivedStateOf {
                val offset = state.draggable.offset.takeUnless { it.isNaN() } ?: peekTop
                (1f - (offset - fullTop) / (peekTop - fullTop)).coerceIn(0f, 1f)
            }
        }

        content()

        // Scrim deepens as the sheet rises, keeping the sheet legible over busy content.
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = colors.scrim.alpha * 0.6f * progress)),
        )

        val corner = lerp(radii.sheet, 24.dp, progress)
        val shape = SquircleShape(corner)
        val base = GlassDefaults.sheet()
        val style = base.copy(blurRadius = lerp(base.blurRadius * 0.7f, base.blurRadius * 1.15f, progress))
        GlassSurface(
            shape = shape,
            style = style,
            modifier = Modifier
                .offset { IntOffset(0, (state.draggable.offset.takeUnless { it.isNaN() } ?: peekTop).roundToInt()) }
                .fillMaxWidth()
                .height(maxHeight - with(density) { fullTop.toDp() })
                .overscroll(overscroll)
                .anchoredDraggable(
                    state = state.draggable,
                    reverseDirection = false,
                    orientation = Orientation.Vertical,
                    overscrollEffect = overscroll,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(state.draggable, animationSpec = motion.smooth()),
                )
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(expandLabel) {
                            val next = if (state.detent == SheetDetent.Peek) SheetDetent.Half else SheetDetent.Full
                            scope.launch { state.settleTo(next, motion) }
                            true
                        },
                        CustomAccessibilityAction(collapseLabel) {
                            val next = if (state.detent == SheetDetent.Full) SheetDetent.Half else SheetDetent.Peek
                            scope.launch { state.settleTo(next, motion) }
                            true
                        },
                    )
                },
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 40.dp, height = 5.dp)
                        .clip(SquircleShape.Pill)
                        .background(colors.textTertiary.copy(alpha = 0.6f))
                        .align(Alignment.CenterHorizontally),
                )
                Column(Modifier.padding(horizontal = CloakTheme.spacing.gutter), content = sheetContent)
            }
        }
    }
}

private const val FULL_TOP_FRACTION = 0.08f
private const val HALF_TOP_FRACTION = 0.5f
