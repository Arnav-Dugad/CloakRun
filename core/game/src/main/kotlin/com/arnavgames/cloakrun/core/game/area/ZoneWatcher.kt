package com.arnavgames.cloakrun.core.game.area

import com.arnavgames.cloakrun.core.game.location.LocationFix

/** Where the player stands relative to the play area edge. */
enum class ZoneStatus { Inside, NearEdge, Outside }

/** A change in [ZoneStatus], for warnings, haptics and the danger vignette. */
data class ZoneTransition(
    val from: ZoneStatus,
    val to: ZoneStatus,
    val distanceToEdgeM: Double,
)

/**
 * Local geofencing against the play area (circle or polygon). Runs on every fix, on the device,
 * so it works with arbitrary polygons and reacts within one fix, unlike platform geofences.
 *
 * Hysteresis stops GPS jitter from flickering the status: a fix must clear a boundary by a margin
 * scaled to its accuracy before the status changes. Inaccurate fixes are ignored.
 */
class ZoneWatcher(
    private val area: PlayArea,
    private val nearEdgeM: Double = 40.0,
    private val maxAccuracyM: Float = 60f,
) {
    var status: ZoneStatus = ZoneStatus.Inside
        private set

    private var initialised = false

    /** Feeds a fix. Returns the transition if the status changed. */
    fun onFix(fix: LocationFix): ZoneTransition? {
        if (fix.accuracyM > maxAccuracyM) return null
        val d = area.signedDistanceToEdge(fix.point)
        val margin = (fix.accuracyM / 2.0).coerceIn(MIN_MARGIN_M, MAX_MARGIN_M)
        val next = if (!initialised) {
            initialised = true
            classify(d)
        } else {
            next(status, d, margin)
        }
        if (next == status) return null
        val transition = ZoneTransition(status, next, d)
        status = next
        return transition
    }

    private fun classify(d: Double): ZoneStatus = when {
        d < 0 -> ZoneStatus.Outside
        d < nearEdgeM -> ZoneStatus.NearEdge
        else -> ZoneStatus.Inside
    }

    private fun next(
        current: ZoneStatus,
        d: Double,
        margin: Double,
    ): ZoneStatus = when (current) {
        ZoneStatus.Inside -> if (d < nearEdgeM - margin) classify(d) else current

        ZoneStatus.NearEdge -> when {
            d < -margin -> ZoneStatus.Outside
            d > nearEdgeM + margin -> ZoneStatus.Inside
            else -> current
        }

        ZoneStatus.Outside -> if (d > margin) classify(d) else current
    }

    private companion object {
        const val MIN_MARGIN_M = 3.0
        const val MAX_MARGIN_M = 15.0
    }
}
