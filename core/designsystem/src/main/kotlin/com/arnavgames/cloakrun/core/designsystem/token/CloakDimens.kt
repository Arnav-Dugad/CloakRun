package com.arnavgames.cloakrun.core.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale on a 4dp rhythm. Generous by default: this is a premium, outdoor, one-handed app. */
@Immutable
data class CloakSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    val huge: Dp = 64.dp,
    /** Horizontal screen margin. */
    val gutter: Dp = 20.dp,
)

/**
 * Corner radii for continuous (squircle) corners. Buttons and chips are fully rounded, which
 * components express by passing [Pill] to the squircle shape.
 */
@Immutable
data class CloakRadii(
    val xs: Dp = 10.dp,
    val s: Dp = 16.dp,
    val m: Dp = 22.dp,
    val card: Dp = 28.dp,
    val sheet: Dp = 40.dp,
) {
    companion object {
        /** Sentinel meaning "half the shorter side". */
        val Pill: Dp = Dp.Infinity
    }
}

/** Sizes that must hold for accessibility and one-handed reach. */
@Immutable
data class CloakSizes(
    /** Minimum touch target. Material and WCAG recommend at least 48dp; outdoors we go larger. */
    val minTouchTarget: Dp = 48.dp,
    val buttonHeight: Dp = 56.dp,
    val buttonHeightLarge: Dp = 64.dp,
    val chipHeight: Dp = 36.dp,
    val icon: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val edgeHighlight: Dp = 1.dp,
    /** Height of the bottom zone that is comfortably reachable with one thumb on a 6.5 inch phone. */
    val thumbZone: Dp = 280.dp,
)

/** Blur radii for glass. Lower tiers are chosen automatically on weak devices (see GlassQuality). */
@Immutable
data class CloakBlur(
    val surface: Dp = 24.dp,
    val sheet: Dp = 36.dp,
    val chrome: Dp = 18.dp,
)
