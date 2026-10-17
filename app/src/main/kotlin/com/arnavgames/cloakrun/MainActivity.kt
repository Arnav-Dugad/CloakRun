package com.arnavgames.cloakrun

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arnavgames.cloakrun.about.AboutScreen
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().handOffToContent()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            CloakTheme(preference = ThemePreference.Night) {
                AboutScreen(versionName = BuildConfig.VERSION_NAME)
            }
        }
    }

    /**
     * Exit transition: the emblem swells slightly while the splash fades, so it hands over to the
     * emblem on the first screen instead of cutting. Skipped when system animations are off.
     */
    private fun SplashScreen.handOffToContent() {
        setOnExitAnimationListener { provider ->
            val animationsOff = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            if (animationsOff) {
                provider.remove()
                return@setOnExitAnimationListener
            }
            val emphasized = PathInterpolator(0.2f, 0f, 0f, 1f)
            val icon: View? = runCatching { provider.iconView }.getOrNull()
            val animators = buildList {
                add(ObjectAnimator.ofFloat(provider.view, View.ALPHA, 1f, 0f).setDuration(SPLASH_FADE_MS))
                if (icon != null) {
                    add(ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.18f).setDuration(SPLASH_FADE_MS))
                    add(ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.18f).setDuration(SPLASH_FADE_MS))
                }
            }
            AnimatorSet().apply {
                playTogether(animators)
                interpolator = emphasized
                doOnEnd { provider.remove() }
                start()
            }
        }
    }

    private companion object {
        const val SPLASH_FADE_MS = 360L
    }
}
