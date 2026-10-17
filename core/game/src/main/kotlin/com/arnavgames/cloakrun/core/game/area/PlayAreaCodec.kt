package com.arnavgames.cloakrun.core.game.area

import com.arnavgames.cloakrun.core.game.geo.GeoPoint

/** Compact, versioned text encoding for storing an area locally. */
object PlayAreaCodec {
    private const val VERSION = "v1"

    fun encode(area: PlayArea): String = when (area) {
        is PlayArea.Circle -> "$VERSION|circle|${area.center.lat},${area.center.lng},${area.radiusM}"
        is PlayArea.Polygon -> "$VERSION|polygon|" + area.vertices.joinToString(";") { "${it.lat},${it.lng}" }
    }

    fun decode(text: String): PlayArea? = runCatching {
        val parts = text.split("|")
        require(parts.size == 3 && parts[0] == VERSION)
        when (parts[1]) {
            "circle" -> {
                val (lat, lng, r) = parts[2].split(",").map(String::toDouble)
                PlayArea.Circle(GeoPoint(lat, lng), r)
            }

            "polygon" -> PlayArea.Polygon(
                parts[2].split(";").map { pair ->
                    val (lat, lng) = pair.split(",").map(String::toDouble)
                    GeoPoint(lat, lng)
                },
            )

            else -> error("unknown shape")
        }
    }.getOrNull()
}
