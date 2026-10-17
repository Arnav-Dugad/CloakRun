package com.arnavgames.cloakrun.core.game.area

import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.geo.LocalFrame
import com.arnavgames.cloakrun.core.game.geo.Planar
import com.arnavgames.cloakrun.core.game.geo.Vec2
import kotlin.math.PI

/** The real-world region a match is played in, drawn by the host. */
sealed interface PlayArea {
    val areaM2: Double

    /** A representative centre (the circle centre, or the polygon's vertex centroid). */
    val center: GeoPoint

    fun contains(p: GeoPoint): Boolean

    /** Distance from [p] to the edge in metres: positive inside, negative outside. */
    fun signedDistanceToEdge(p: GeoPoint): Double

    /** Vertices for drawing (a circle is approximated by [segments] points). */
    fun outline(segments: Int = 96): List<GeoPoint>

    data class Circle(override val center: GeoPoint, val radiusM: Double) : PlayArea {
        init {
            require(radiusM > 0) { "radius must be positive" }
        }

        override val areaM2: Double get() = PI * radiusM * radiusM

        override fun contains(p: GeoPoint): Boolean = Geo.distance(center, p) <= radiusM

        override fun signedDistanceToEdge(p: GeoPoint): Double = radiusM - Geo.distance(center, p)

        override fun outline(segments: Int): List<GeoPoint> =
            (0 until segments).map { i -> Geo.destination(center, 360.0 * i / segments, radiusM) }
    }

    data class Polygon(val vertices: List<GeoPoint>) : PlayArea {
        init {
            require(vertices.size >= 3) { "a polygon needs at least 3 vertices" }
        }

        override val center: GeoPoint = GeoPoint(vertices.map { it.lat }.average(), vertices.map { it.lng }.average())

        private val frame = LocalFrame(center)
        private val ring: List<Vec2> = vertices.map(frame::toLocal)

        override val areaM2: Double get() = Planar.area(ring)

        val isSelfIntersecting: Boolean get() = Planar.isSelfIntersecting(ring)

        override fun contains(p: GeoPoint): Boolean = Planar.contains(ring, frame.toLocal(p))

        override fun signedDistanceToEdge(p: GeoPoint): Double {
            val local = frame.toLocal(p)
            val d = Planar.distanceToBoundary(ring, local)
            return if (Planar.contains(ring, local)) d else -d
        }

        override fun outline(segments: Int): List<GeoPoint> = vertices
    }
}

/** Limits the host must respect when drawing an area. Tunable from Remote Config later. */
data class PlayAreaRules(
    val minAreaM2: Double = 20_000.0,
    val maxAreaM2: Double = 3_000_000.0,
    val minVertices: Int = 3,
    val maxVertices: Int = 24,
) {
    fun validate(area: PlayArea): PlayAreaCheck = when {
        area is PlayArea.Polygon && area.vertices.size < minVertices -> PlayAreaCheck.TooFewVertices
        area is PlayArea.Polygon && area.vertices.size > maxVertices -> PlayAreaCheck.TooManyVertices
        area is PlayArea.Polygon && area.isSelfIntersecting -> PlayAreaCheck.SelfIntersecting
        area.areaM2 < minAreaM2 -> PlayAreaCheck.TooSmall
        area.areaM2 > maxAreaM2 -> PlayAreaCheck.TooLarge
        else -> PlayAreaCheck.Valid
    }

    /** The largest circle radius that fits [maxAreaM2]. */
    val maxCircleRadiusM: Double get() = kotlin.math.sqrt(maxAreaM2 / PI)

    val minCircleRadiusM: Double get() = kotlin.math.sqrt(minAreaM2 / PI)
}

enum class PlayAreaCheck {
    Valid,
    TooSmall,
    TooLarge,
    SelfIntersecting,
    TooFewVertices,
    TooManyVertices,
    ;

    val isValid: Boolean get() = this == Valid
}

/** Formats an area for the live readout: square metres below a hectare, hectares, then square kilometres. */
fun formatArea(m2: Double): AreaReadout = when {
    m2 < 10_000 -> AreaReadout(m2, AreaUnit.SquareMetres)
    m2 < 1_000_000 -> AreaReadout(m2 / 10_000, AreaUnit.Hectares)
    else -> AreaReadout(m2 / 1_000_000, AreaUnit.SquareKilometres)
}

data class AreaReadout(val value: Double, val unit: AreaUnit)

enum class AreaUnit { SquareMetres, Hectares, SquareKilometres }
