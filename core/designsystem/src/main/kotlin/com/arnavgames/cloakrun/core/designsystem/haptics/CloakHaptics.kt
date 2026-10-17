package com.arnavgames.cloakrun.core.designsystem.haptics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

/** The CloakRun haptic vocabulary. Each cue has one meaning everywhere in the app. */
enum class HapticCue {
    /** UI feedback: a toggle, a tab, a chip. */
    Tick,

    /** A press that commits an action. */
    Confirm,

    /** Radar ping or sonar return. */
    Ping,

    /** A seeker is close: two low taps. */
    Near,

    /** You were caught, or you caught someone. */
    Caught,

    /** A power was collected or activated: a rising swell. */
    PowerUp,

    /** A countdown second. */
    CountdownTick,

    /** One lub-dub of the proximity heartbeat. */
    Heartbeat,

    /** Something went wrong. */
    Error,
}

/**
 * Plays [HapticCue]s with `VibrationEffect.Composition` primitives on devices that support them
 * (Android 11+ with a capable actuator), falling back to predefined effects (Android 10) and then
 * to plain waveforms. Respects [enabled] (the in-app setting) and the system's
 * "vibration and haptics" toggle, which the platform enforces.
 */
class CloakHaptics internal constructor(private val vibrator: Vibrator?, private val enabled: Boolean) {
    fun play(cue: HapticCue) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        val effect = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> composed(v, cue) ?: predefinedOrWaveform(cue)
            else -> predefinedOrWaveform(cue)
        }
        v.vibrate(effect)
    }

    // Every primitive used here is a VibrationEffect.Composition constant; lint cannot trace them through the list.
    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun composed(v: Vibrator, cue: HapticCue): VibrationEffect? {
        val s31 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val low = if (s31) VibrationEffect.Composition.PRIMITIVE_LOW_TICK else VibrationEffect.Composition.PRIMITIVE_TICK
        val thud = if (s31) VibrationEffect.Composition.PRIMITIVE_THUD else VibrationEffect.Composition.PRIMITIVE_CLICK
        val steps: List<Triple<Int, Float, Int>> = when (cue) {
            HapticCue.Tick -> listOf(Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 0))

            HapticCue.Confirm -> listOf(
                Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f, 0),
                Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 60),
            )

            HapticCue.Ping -> listOf(
                Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.55f, 0),
                Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.25f, 90),
            )

            HapticCue.Near -> listOf(Triple(low, 0.7f, 0), Triple(low, 0.9f, 90))

            HapticCue.Caught -> listOf(
                Triple(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.8f, 0),
                Triple(thud, 1f, 30),
                Triple(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.6f, 40),
            )

            HapticCue.PowerUp -> listOf(
                Triple(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.6f, 0),
                Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 20),
            )

            HapticCue.CountdownTick -> listOf(Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 0))

            HapticCue.Heartbeat -> listOf(Triple(thud, 0.85f, 0), Triple(thud, 0.5f, 140))

            HapticCue.Error -> listOf(
                Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 0),
                Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 70),
                Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 70),
            )
        }
        val primitives = steps.map { it.first }.distinct().toIntArray()
        if (!v.areAllPrimitivesSupported(*primitives)) return null
        val composition = VibrationEffect.startComposition()
        for ((primitive, scale, delay) in steps) composition.addPrimitive(primitive, scale, delay)
        return composition.compose()
    }

    private fun predefinedOrWaveform(cue: HapticCue): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val predefined = when (cue) {
                HapticCue.Tick -> VibrationEffect.EFFECT_TICK
                HapticCue.Confirm, HapticCue.CountdownTick, HapticCue.Ping -> VibrationEffect.EFFECT_CLICK
                HapticCue.Caught -> VibrationEffect.EFFECT_HEAVY_CLICK
                HapticCue.Near, HapticCue.Heartbeat, HapticCue.Error -> VibrationEffect.EFFECT_DOUBLE_CLICK
                HapticCue.PowerUp -> null
            }
            if (predefined != null) return VibrationEffect.createPredefined(predefined)
        }
        val (timings, amplitudes) = when (cue) {
            HapticCue.Tick -> longArrayOf(0, 10) to intArrayOf(0, 80)
            HapticCue.Confirm, HapticCue.CountdownTick -> longArrayOf(0, 18) to intArrayOf(0, 180)
            HapticCue.Ping -> longArrayOf(0, 14, 80, 8) to intArrayOf(0, 160, 0, 70)
            HapticCue.Near, HapticCue.Heartbeat -> longArrayOf(0, 24, 120, 18) to intArrayOf(0, 200, 0, 120)
            HapticCue.Caught -> longArrayOf(0, 60, 40, 30) to intArrayOf(0, 255, 0, 140)
            HapticCue.PowerUp -> longArrayOf(0, 30, 0, 30, 0, 40) to intArrayOf(0, 60, 0, 140, 0, 255)
            HapticCue.Error -> longArrayOf(0, 12, 60, 12, 60, 12) to intArrayOf(0, 180, 0, 180, 0, 180)
        }
        return VibrationEffect.createWaveform(timings, amplitudes, -1)
    }

    companion object {
        /** A no-op instance for previews and tests. */
        val Silent = CloakHaptics(vibrator = null, enabled = false)

        fun create(context: Context, enabled: Boolean): CloakHaptics {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService<VibratorManager>()?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService<Vibrator>()
            }
            return CloakHaptics(vibrator, enabled)
        }
    }
}

val LocalCloakHaptics = staticCompositionLocalOf { CloakHaptics.Silent }

@Composable
fun rememberCloakHaptics(enabled: Boolean): CloakHaptics {
    val context = LocalContext.current
    return remember(context, enabled) { CloakHaptics.create(context.applicationContext, enabled) }
}
