package com.arnavgames.cloakrun.core.game.location

import com.arnavgames.cloakrun.core.game.geo.GeoPoint

/** Where a fix came from. Simulated fixes exist only in debug builds. */
enum class FixSource { Device, Simulated }

/**
 * One position sample. Exact fixes never leave the device: they feed the local map, the
 * on-device replay and the fuzzing step that produces what other players see.
 */
data class LocationFix(
    val point: GeoPoint,
    val accuracyM: Float,
    val timeMillis: Long,
    val speedMps: Float? = null,
    val bearingDeg: Float? = null,
    /** The platform reported a mock provider (developer "mock location app"). Flagged, not banned. */
    val isMock: Boolean = false,
    val source: FixSource = FixSource.Device,
)

/**
 * What the player is doing, which decides how often we need their position. Ordered from least
 * to most demanding: when several callers are active, the highest ordinal wins.
 */
enum class TrackingMode {
    /** Not tracking at all. Location is never read outside the modes below. */
    Off,

    /** In a lobby before the match: occasional fixes to show distance to the play area. */
    Lobby,

    /** Looking at the map or drawing a play area, app in the foreground. */
    Scouting,

    /** In an active match, including with the screen off (foreground service). */
    Match,
}

enum class FixPriority { HighAccuracy, Balanced, LowPower }

/** A concrete request for the location provider. */
data class SamplingPlan(
    val intervalMs: Long,
    val minIntervalMs: Long,
    val minDistanceM: Float,
    val priority: FixPriority,
    /** Allow the provider to batch fixes this long while the screen is off, to save power. */
    val maxBatchDelayMs: Long,
)

data class SamplingInput(
    val mode: TrackingMode,
    /** Distance band to the nearest opponent if known (from fuzzed zones), metres. */
    val nearestOpponentM: Double? = null,
    val batteryPercent: Int = 100,
    val charging: Boolean = false,
    val powerSaveMode: Boolean = false,
    /** The device has barely moved over the last minute. */
    val stationary: Boolean = false,
    val screenOn: Boolean = true,
)

/**
 * Adaptive sampling: fast when the chase is close, slow when idle, frugal on low battery.
 * Pure and deterministic so it can be unit tested exhaustively.
 */
object SamplingPolicy {
    const val CLOSE_M = 75.0
    const val NEAR_M = 200.0
    const val LOW_BATTERY = 20
    const val CRITICAL_BATTERY = 10

    fun plan(input: SamplingInput): SamplingPlan? {
        if (input.mode == TrackingMode.Off) return null
        val distance = input.nearestOpponentM ?: Double.MAX_VALUE
        val close = distance <= CLOSE_M
        val near = distance <= NEAR_M
        var step = Step(baseInterval(input.mode, close, near), basePriority(input.mode))
        if (input.stationary && !close) step = step.copy(intervalMs = (step.intervalMs * 2).coerceAtMost(10_000L))
        step = applyBattery(step, input, close, near)
        step = applyPowerSaver(step, input, close, near)
        val batch = if (!input.screenOn && !near && input.mode == TrackingMode.Match) step.intervalMs * 3 else 0L
        return SamplingPlan(
            intervalMs = step.intervalMs,
            minIntervalMs = (step.intervalMs / 2).coerceAtLeast(500L),
            minDistanceM = if (close) 0f else 2f,
            priority = step.priority,
            maxBatchDelayMs = batch,
        )
    }

    private data class Step(val intervalMs: Long, val priority: FixPriority)

    private fun baseInterval(
        mode: TrackingMode,
        close: Boolean,
        near: Boolean,
    ): Long = when (mode) {
        TrackingMode.Off, TrackingMode.Lobby -> 10_000L

        TrackingMode.Scouting -> 2_000L

        TrackingMode.Match -> when {
            close -> 1_000L
            near -> 2_000L
            else -> 4_000L
        }
    }

    private fun basePriority(mode: TrackingMode): FixPriority =
        if (mode == TrackingMode.Lobby) FixPriority.Balanced else FixPriority.HighAccuracy

    private fun applyBattery(
        step: Step,
        input: SamplingInput,
        close: Boolean,
        near: Boolean,
    ): Step {
        if (input.charging) return step
        var s = step
        if (input.batteryPercent <= LOW_BATTERY && !close) {
            s = s.copy(intervalMs = s.intervalMs * 3 / 2, priority = if (near) s.priority else FixPriority.Balanced)
        }
        if (input.batteryPercent <= CRITICAL_BATTERY) {
            s = Step(s.intervalMs.coerceAtLeast(5_000L), if (close) FixPriority.Balanced else FixPriority.LowPower)
        }
        return s
    }

    private fun applyPowerSaver(
        step: Step,
        input: SamplingInput,
        close: Boolean,
        near: Boolean,
    ): Step {
        if (!input.powerSaveMode || close) return step
        val priority = if (step.priority == FixPriority.HighAccuracy && !near) FixPriority.Balanced else step.priority
        return Step(step.intervalMs.coerceAtLeast(3_000L), priority)
    }
}
