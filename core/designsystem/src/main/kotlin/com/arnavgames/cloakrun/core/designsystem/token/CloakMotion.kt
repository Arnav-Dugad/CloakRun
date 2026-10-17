package com.arnavgames.cloakrun.core.designsystem.token

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable

/**
 * Motion tokens. All CloakRun motion is spring-based. When [reduced] is true (the system
 * "remove animations" setting, or the in-app override) every spec collapses to an instant snap,
 * and decorative loops must stop.
 */
@Immutable
data class CloakMotion(val reduced: Boolean = false) {
    /** UI feedback: presses, toggles, chips. Quick and barely bouncy. */
    fun <T> snappy(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.82f, stiffness = 900f, visibilityThreshold = visibilityThreshold)

    /** Layout and position changes: sheets, cards, list moves. */
    fun <T> smooth(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.9f, stiffness = 380f, visibilityThreshold = visibilityThreshold)

    /** Playful, physical motion: squash and stretch, magnetic snaps, orbit joins. */
    fun <T> bouncy(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.55f, stiffness = 420f, visibilityThreshold = visibilityThreshold)

    /** Slow, cinematic moves: map fly-ins, hero reveals, the breathing fuzzy zone. */
    fun <T> gentle(visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 1f, stiffness = Spring.StiffnessVeryLow, visibilityThreshold = visibilityThreshold)

    /** Raw spring parameters for physics that is integrated by hand (for example orbiting avatars). */
    val orbitSpring: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 140f)

    companion object {
        /** Durations for the few non-spring effects (shader sweeps, haptic-synced pulses). */
        const val SWEEP_PERIOD_MS = 2_400
        const val BREATH_PERIOD_MS = 3_200
        const val SHIMMER_PERIOD_MS = 1_800
    }
}
