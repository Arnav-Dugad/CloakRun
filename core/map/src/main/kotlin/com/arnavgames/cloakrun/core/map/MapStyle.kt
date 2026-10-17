package com.arnavgames.cloakrun.core.map

import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.sun.SunClock

/**
 * The two CloakRun map styles, authored in scripts/map/gen_styles.py and shipped as assets.
 * Both read OpenFreeMap vector tiles (OpenMapTiles schema).
 */
enum class MapStyle(val uri: String) {
    Night("asset://styles/cloakrun-night.json"),
    Day("asset://styles/cloakrun-day.json"),
    ;

    companion object {
        /** Night between local sunset and sunrise at [at], Day otherwise. */
        fun forTime(nowMillis: Long, at: GeoPoint): MapStyle = if (SunClock.isNight(nowMillis, at)) Night else Day
    }
}

/** The user's map style preference. [Auto] follows local sunset and sunrise. */
enum class MapStylePreference { Auto, Night, Day }
