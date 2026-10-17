package com.arnavgames.cloakrun.core.game.safety

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.geo.LocalFrame
import com.arnavgames.cloakrun.core.game.geo.Planar
import com.arnavgames.cloakrun.core.game.geo.Vec2

/** Why a place inside the play area is off limits. */
enum class HazardKind {
    /** Lakes, rivers, ponds, pools. */
    Water,

    /** Motorways, trunk and primary roads: fast traffic. */
    MajorRoad,

    /** Railway and tram lines. */
    Rail,

    /** Building footprints: indoors is never part of the game. */
    Building,

    /** Land that is rarely public: military, industrial, quarries, rail yards, garages, dams, schools. */
    Restricted,
}

/** A no-go zone, either an area (water, buildings, restricted land) or a corridor along a line (roads, rail). */
sealed interface Hazard {
    val kind: HazardKind

    data class Area(override val kind: HazardKind, val ring: List<GeoPoint>) : Hazard

    data class Corridor(
        override val kind: HazardKind,
        val line: List<GeoPoint>,
        val halfWidthM: Double,
    ) : Hazard

    companion object {
        /** Half-widths for line hazards: the carriageway plus a safety margin either side. */
        fun corridorHalfWidth(kind: HazardKind, roadClass: String? = null): Double = when (kind) {
            HazardKind.Rail -> 8.0

            HazardKind.MajorRoad -> when (roadClass) {
                "motorway" -> 18.0
                "trunk" -> 14.0
                else -> 10.0
            }

            else -> 6.0
        }
    }
}

/** What the safety pass found inside a play area. */
data class SafetySummary(
    val hazards: List<Hazard>,
    val countByKind: Map<HazardKind, Int>,
    /** Fraction of the play area covered by no-go zones, estimated by sampling. */
    val coveredFraction: Double,
) {
    /** Too much of the area is off limits to play fairly and safely. */
    val isMostlyUnsafe: Boolean get() = coveredFraction > UNSAFE_FRACTION

    companion object {
        const val UNSAFE_FRACTION = 0.4
        val Empty = SafetySummary(emptyList(), emptyMap(), 0.0)
    }
}

/**
 * Hazards prepared for fast point queries in a local metric frame. Used by the game (zone
 * warnings, cache-point placement) and to summarise an area for the host.
 */
class HazardMap(val hazards: List<Hazard>, origin: GeoPoint) {
    private val frame = LocalFrame(origin)
    private val areas = hazards.filterIsInstance<Hazard.Area>().map { h -> h to h.ring.map(frame::toLocal) }
    private val corridors = hazards.filterIsInstance<Hazard.Corridor>().map { h -> h to h.line.map(frame::toLocal) }

    /** The first hazard covering [point], or null where it is safe to be. */
    fun hazardAt(point: GeoPoint): Hazard? {
        val p = frame.toLocal(point)
        areas.firstOrNull { (_, ring) -> Planar.contains(ring, p) }?.let { return it.first }
        return corridors.firstOrNull { (h, line) -> distanceToPolyline(line, p) <= h.halfWidthM }?.first
    }

    fun isSafe(point: GeoPoint): Boolean = hazardAt(point) == null

    /** Keeps only hazards that touch [area] and estimates how much of it they cover. */
    fun summarise(area: PlayArea, samples: Int = 1_600): SafetySummary {
        val touching = hazards.filter { h ->
            when (h) {
                is Hazard.Area -> h.ring.any(area::contains) || Planar.contains(h.ring.map(frame::toLocal), frame.toLocal(area.center))
                is Hazard.Corridor -> h.line.any { area.signedDistanceToEdge(it) > -h.halfWidthM }
            }
        }
        val local = HazardMap(touching, area.center)
        val ring = area.outline(64).map { frame.toLocal(it) }
        val xs = ring.map(Vec2::x)
        val ys = ring.map(Vec2::y)
        val side = kotlin.math.sqrt(samples.toDouble()).toInt().coerceAtLeast(10)
        var inside = 0
        var covered = 0
        for (i in 0 until side) {
            for (j in 0 until side) {
                val v = Vec2(
                    xs.min() + (xs.max() - xs.min()) * (i + 0.5) / side,
                    ys.min() + (ys.max() - ys.min()) * (j + 0.5) / side,
                )
                val g = frame.toGeo(v)
                if (!area.contains(g)) continue
                inside++
                if (!local.isSafe(g)) covered++
            }
        }
        return SafetySummary(
            hazards = touching,
            countByKind = touching.groupingBy { it.kind }.eachCount(),
            coveredFraction = if (inside == 0) 0.0 else covered.toDouble() / inside,
        )
    }

    private fun distanceToPolyline(line: List<Vec2>, p: Vec2): Double {
        if (line.size == 1) return (line[0] - p).length
        var best = Double.MAX_VALUE
        for (i in 0 until line.lastIndex) best = minOf(best, Planar.distanceToSegment(p, line[i], line[i + 1]))
        return best
    }
}
