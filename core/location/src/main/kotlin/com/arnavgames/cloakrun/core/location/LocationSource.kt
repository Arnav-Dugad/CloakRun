package com.arnavgames.cloakrun.core.location

import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.game.location.SamplingPlan
import kotlinx.coroutines.flow.Flow

/** Produces location fixes according to a [SamplingPlan]. Collection starts and stops updates. */
fun interface LocationSource {
    fun fixes(plan: SamplingPlan): Flow<LocationFix>
}

/** Thrown into the flow when location cannot be read (no permission, or location services off). */
class LocationUnavailableException(reason: String) : IllegalStateException(reason)
