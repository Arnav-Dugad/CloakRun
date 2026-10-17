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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.LocalTopChromeInset
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import com.arnavgames.cloakrun.core.location.LocationRepository
import com.arnavgames.cloakrun.core.location.TrackingController
import com.arnavgames.cloakrun.core.map.LocalMapOverlayContributors
import com.arnavgames.cloakrun.navigation.CloakRunNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var locationRepository: LocationRepository

    @Inject lateinit var trackingController: TrackingController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().handOffToContent()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val overlays = debugMapOverlays(locationRepository, trackingController)
        setContent {
            CloakTheme(preference = ThemePreference.Night) {
                CompositionLocalProvider(LocalMapOverlayContributors provides overlays) {
                    val sharing by trackingController.isTracking.collectAsStateWithLifecycle()
                    val inset by animateDpAsState(if (sharing) SHARING_INSET else 0.dp, label = "chromeInset")
                    Box(Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalTopChromeInset provides inset) {
                            CloakRunNavigation(versionName = BuildConfig.VERSION_NAME)
                        }
                        LocationSharingIndicator(sharing, Modifier.align(Alignment.TopCenter))
                    }
                }
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
        val SHARING_INSET = 56.dp
    }
}
