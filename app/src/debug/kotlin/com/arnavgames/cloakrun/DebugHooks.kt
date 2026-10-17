package com.arnavgames.cloakrun

import com.arnavgames.cloakrun.core.location.LocationRepository
import com.arnavgames.cloakrun.core.location.TrackingController
import com.arnavgames.cloakrun.core.map.MapOverlayContributor
import com.arnavgames.cloakrun.feature.devtools.simulation.SimulationOverlay

/** Debug builds add Simulation Mode and the debug HUD to every map. */
internal fun debugMapOverlays(repository: LocationRepository, tracking: TrackingController): List<MapOverlayContributor> =
    listOf(SimulationOverlay(repository, tracking))
