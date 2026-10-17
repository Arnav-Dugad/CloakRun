package com.arnavgames.cloakrun.core.designsystem.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

/**
 * How much glass the device can afford. Chosen once per process from the hardware, and lowered
 * further when the user enables Performance mode.
 *
 * - [Full]: real backdrop blur (RenderEffect, API 31+), grain and specular.
 * - [Reduced]: blur with a smaller radius and lower-resolution input, no grain.
 * - [Tint]: no blur at all; a denser frosted tint stands in, so text contrast is preserved.
 */
enum class GlassQuality {
    Full,
    Reduced,
    Tint,
    ;

    val blurs: Boolean get() = this != Tint

    companion object {
        /**
         * Picks a tier from the platform: blur needs API 31 (RenderEffect). Low-RAM devices and
         * devices with a small per-app memory class get [Reduced].
         */
        fun detect(context: Context): GlassQuality {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return Tint
            val am = context.getSystemService<ActivityManager>() ?: return Reduced
            return when {
                am.isLowRamDevice -> Tint
                am.memoryClass < LOW_MEMORY_CLASS_MB -> Reduced
                else -> Full
            }
        }

        private const val LOW_MEMORY_CLASS_MB = 192
    }
}

/** The device tier, lowered to [GlassQuality.Tint] when [performanceMode] is on. */
@Composable
fun rememberGlassQuality(performanceMode: Boolean): GlassQuality {
    val context = LocalContext.current
    val detected = remember(context) { GlassQuality.detect(context) }
    return if (performanceMode) GlassQuality.Tint else detected
}
