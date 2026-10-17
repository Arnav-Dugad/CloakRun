package com.arnavgames.cloakrun

import com.arnavgames.cloakrun.core.location.LocationRepository
import com.arnavgames.cloakrun.core.location.TrackingController
import com.arnavgames.cloakrun.core.map.MapOverlayContributor

/** Release builds ship no debug tooling. */
@Suppress("UNUSED_PARAMETER")
internal fun debugMapOverlays(repository: LocationRepository, tracking: TrackingController): List<MapOverlayContributor> = emptyList()
