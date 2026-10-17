package com.arnavgames.cloakrun

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.arnavgames.cloakrun.core.data.PlayerPrefs
import com.arnavgames.cloakrun.core.data.PlayerState
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.LocalTopChromeInset
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import com.arnavgames.cloakrun.core.game.lobby.InviteLink
import com.arnavgames.cloakrun.core.location.LocationRepository
import com.arnavgames.cloakrun.core.location.TrackingController
import com.arnavgames.cloakrun.core.map.LocalMapOverlayContributors
import com.arnavgames.cloakrun.feature.home.HomePlayer
import com.arnavgames.cloakrun.navigation.CloakRunNavigation
import com.arnavgames.cloakrun.navigation.NavigationInput
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var locationRepository: LocationRepository

    @Inject lateinit var trackingController: TrackingController

    @Inject lateinit var playerPrefs: PlayerPrefs

    /** An invite link that opened the app, waiting for the graph to handle it. */
    private var pendingInvite by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.handOffToContent()
        if (savedInstanceState == null) pendingInvite = intent.inviteLink()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val overlays = debugMapOverlays(locationRepository, trackingController)
        var player: PlayerState? by mutableStateOf(null)
        // Keep the splash up until we know whether to show onboarding or home (a few milliseconds).
        splash.setKeepOnScreenCondition { player == null }
        lifecycleScope.launch { playerPrefs.state.collect { player = it } }
        setContent {
            CloakTheme(preference = ThemePreference.Night) {
                CompositionLocalProvider(LocalMapOverlayContributors provides overlays) {
                    val sharing by trackingController.isTracking.collectAsStateWithLifecycle()
                    val inset by animateDpAsState(if (sharing) SHARING_INSET else 0.dp, label = "chromeInset")
                    var updateSheet by rememberSaveable { mutableStateOf(false) }
                    AppUpdates.LaunchCheck()
                    Box(Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalTopChromeInset provides inset) {
                            player?.let { p ->
                                CloakRunNavigation(
                                    NavigationInput(
                                        onboarded = p.onboarded,
                                        player = p.profile?.let { HomePlayer(it.name, it.avatarSeed) },
                                        activeMatchId = p.activeMatchId,
                                        pendingInvite = pendingInvite,
                                        onInviteHandled = { pendingInvite = null },
                                        versionName = BuildConfig.VERSION_NAME,
                                        onOpenUpdates = { updateSheet = true },
                                    ),
                                )
                            }
                        }
                        LocationSharingIndicator(sharing, Modifier.align(Alignment.TopCenter))
                        AppUpdates.SheetHost(visible = updateSheet, onDismiss = { updateSheet = false })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.inviteLink()?.let { pendingInvite = it }
    }

    /** The invite URL from an App Link (https://arnav-dugad.github.io/join/?code=...), if any. */
    private fun Intent?.inviteLink(): String? =
        this?.takeIf { it.action == Intent.ACTION_VIEW }?.data?.toString()?.takeIf { InviteLink.parse(it) != null }

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
