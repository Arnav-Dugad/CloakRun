package com.arnavgames.cloakrun.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.arnavgames.cloakrun.AppUpdates
import com.arnavgames.cloakrun.about.AboutScreen
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.feature.home.HomeActions
import com.arnavgames.cloakrun.feature.home.HomePlayer
import com.arnavgames.cloakrun.feature.home.HomeScreen
import com.arnavgames.cloakrun.feature.lobby.HostSetupRoute
import com.arnavgames.cloakrun.feature.lobby.JoinRoute
import com.arnavgames.cloakrun.feature.lobby.LobbyRoute
import com.arnavgames.cloakrun.feature.onboarding.OnboardingRoute
import com.arnavgames.cloakrun.feature.playarea.PlayAreaRoute
import com.arnavgames.cloakrun.feature.profile.ProfileRoute
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingKey : NavKey

@Serializable
data object HomeKey : NavKey

@Serializable
data object PlayAreaKey : NavKey

@Serializable
data object AboutKey : NavKey

@Serializable
data object ProfileKey : NavKey

@Serializable
data object HostKey : NavKey

/** [invite] is an invite link or code to join straight away (from an App Link or a QR scan). */
@Serializable
data class JoinKey(val invite: String? = null) : NavKey

@Serializable
data class LobbyKey(val matchId: String) : NavKey

/** What the shell knows before the graph starts. */
class NavigationInput(
    val onboarded: Boolean,
    val player: HomePlayer?,
    val activeMatchId: String?,
    val pendingInvite: String?,
    val onInviteHandled: () -> Unit,
    val versionName: String,
    val onOpenUpdates: () -> Unit,
)

/**
 * The app's navigation graph (Navigation 3). The back stack is plain saveable state; transitions
 * are springs, and predictive back shows a scaled preview of the screen underneath.
 */
@Composable
fun CloakRunNavigation(input: NavigationInput, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(if (input.onboarded) HomeKey else OnboardingKey)
    val reduced = CloakTheme.motion.reduced
    // An invite (App Link or QR) opens the join screen once the player has a profile.
    LaunchedEffect(input.pendingInvite, input.onboarded) {
        val invite = input.pendingInvite ?: return@LaunchedEffect
        if (!input.onboarded) return@LaunchedEffect
        backStack.resetTo(HomeKey)
        backStack.add(JoinKey(invite))
        input.onInviteHandled()
    }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { forward(reduced) },
        popTransitionSpec = { backward(reduced) },
        predictivePopTransitionSpec = { backward(reduced) },
        entryProvider = entryProvider {
            entry<OnboardingKey> { OnboardingRoute(onFinish = { backStack.resetTo(HomeKey) }) }
            entry<HomeKey> {
                HomeScreen(
                    actions = HomeActions(
                        onDesignArea = { backStack.add(PlayAreaKey) },
                        onAbout = { backStack.add(AboutKey) },
                        onHost = { backStack.add(HostKey) },
                        onJoin = { backStack.add(JoinKey()) },
                        onReturnToLobby = { input.activeMatchId?.let { backStack.add(LobbyKey(it)) } },
                        onProfile = { backStack.add(ProfileKey) },
                    ),
                    player = input.player?.copy(inLobby = input.activeMatchId != null),
                    banner = { AppUpdates.HomeBanner(input.onOpenUpdates, Modifier) },
                )
            }
            entry<PlayAreaKey> { PlayAreaRoute(onBack = { backStack.removeLastOrNull() }) }
            entry<AboutKey> {
                AboutScreen(versionName = input.versionName, updatesCard = { AppUpdates.AboutCard(input.onOpenUpdates, Modifier) })
            }
            entry<ProfileKey> {
                ProfileRoute(onBack = { backStack.removeLastOrNull() }, onAccountDelete = { backStack.resetTo(OnboardingKey) })
            }
            entry<HostKey> {
                HostSetupRoute(
                    onBack = { backStack.removeLastOrNull() },
                    onDesignArea = { backStack.add(PlayAreaKey) },
                    onLobbyOpen = { backStack.replaceTop(LobbyKey(it)) },
                )
            }
            entry<JoinKey> { key ->
                JoinRoute(
                    initialInvite = key.invite,
                    onBack = { backStack.removeLastOrNull() },
                    onJoin = { backStack.replaceTop(LobbyKey(it)) },
                )
            }
            entry<LobbyKey> { key -> LobbyRoute(matchId = key.matchId, onHome = { backStack.resetTo(HomeKey) }) }
        },
    )
}

private fun NavBackStack<NavKey>.resetTo(key: NavKey) {
    clear()
    add(key)
}

private fun NavBackStack<NavKey>.replaceTop(key: NavKey) {
    removeLastOrNull()
    add(key)
}

private fun forward(reduced: Boolean): ContentTransform = if (reduced) {
    fadeIn(spring()) togetherWith fadeOut(spring())
} else {
    (
        slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f, visibilityThreshold = IntOffset(1, 1))) {
            it / 5
        } + fadeIn(spring())
        ) togetherWith
        (scaleOut(spring(stiffness = 380f), targetScale = 0.94f) + fadeOut(spring()))
}

private fun backward(reduced: Boolean): ContentTransform = if (reduced) {
    fadeIn(spring()) togetherWith fadeOut(spring())
} else {
    (scaleIn(spring(stiffness = 380f), initialScale = 0.94f) + fadeIn(spring())) togetherWith
        (
            slideOutHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f, visibilityThreshold = IntOffset(1, 1))) { it / 5 } +
                fadeOut(spring())
            )
}
