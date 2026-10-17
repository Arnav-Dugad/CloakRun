package com.arnavgames.cloakrun.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.arnavgames.cloakrun.core.game.area.PlayArea

/**
 * Extra map layers supplied by optional modules. Debug builds use it for Simulation Mode and the
 * debug HUD; release builds provide none, so no debug code is referenced from release paths.
 */
interface MapOverlayContributor {
    @Composable
    fun Overlay(state: CloakMapState, playArea: PlayArea?)
}

val LocalMapOverlayContributors = staticCompositionLocalOf<List<MapOverlayContributor>> { emptyList() }
