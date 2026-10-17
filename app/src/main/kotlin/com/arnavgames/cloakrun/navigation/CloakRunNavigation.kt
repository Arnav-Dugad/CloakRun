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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.arnavgames.cloakrun.about.AboutScreen
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.feature.home.HomeScreen
import com.arnavgames.cloakrun.feature.playarea.PlayAreaRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data object PlayAreaKey : NavKey

@Serializable
data object AboutKey : NavKey

/**
 * The app's navigation graph (Navigation 3). The back stack is plain saveable state; transitions
 * are springs, and predictive back shows a scaled preview of the screen underneath.
 */
@Composable
fun CloakRunNavigation(versionName: String, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeKey)
    val reduced = CloakTheme.motion.reduced
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
            entry<HomeKey> {
                HomeScreen(onDesignArea = { backStack.add(PlayAreaKey) }, onAbout = { backStack.add(AboutKey) })
            }
            entry<PlayAreaKey> { PlayAreaRoute(onBack = { backStack.removeLastOrNull() }) }
            entry<AboutKey> { AboutScreen(versionName = versionName) }
        },
    )
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
