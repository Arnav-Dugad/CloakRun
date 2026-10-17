package com.arnavgames.cloakrun.core.designsystem.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

/**
 * Device tilt as two values in -1..1: [x] for left and right roll and [y] for forward and back pitch.
 * Zero is the phone held at a comfortable reading angle.
 */
@Immutable
data class Tilt(val x: Float, val y: Float) {
    companion object {
        val Level = Tilt(0f, 0f)
    }
}

/**
 * Observes the gravity sensor (falling back to the accelerometer), low-pass filtered so glass
 * highlights glide instead of jitter. Returns a constant [Tilt.Level] when [enabled] is false
 * (reduced motion, or no sensor), so callers never need to special-case it.
 */
@Composable
fun rememberDeviceTilt(enabled: Boolean): State<Tilt> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(Tilt.Level) }
    DisposableEffect(context, enabled) {
        if (!enabled) {
            state.value = Tilt.Level
            return@DisposableEffect onDispose { }
        }
        val listener = context.registerTiltListener { state.value = it }
        onDispose { listener?.let { context.getSystemService<SensorManager>()?.unregisterListener(it) } }
    }
    return state
}

private fun Context.registerTiltListener(onTilt: (Tilt) -> Unit): SensorEventListener? {
    val manager = getSystemService<SensorManager>() ?: return null
    val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ?: return null
    val listener = object : SensorEventListener {
        private var fx = 0f
        private var fy = 0f

        override fun onSensorChanged(event: SensorEvent) {
            // Gravity in m/s^2. x: roll, y: pitch. A phone held upright at reading angle has y near 7.
            val rawX = (-event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            val rawY = ((event.values[1] - READING_PITCH) / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            fx += (rawX - fx) * SMOOTHING
            fy += (rawY - fy) * SMOOTHING
            onTilt(Tilt(fx, fy))
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    return listener
}

private const val READING_PITCH = 7f
private const val SMOOTHING = 0.12f
