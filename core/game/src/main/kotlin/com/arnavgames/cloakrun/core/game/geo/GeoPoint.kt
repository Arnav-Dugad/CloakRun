package com.arnavgames.cloakrun.core.game.geo

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A WGS84 coordinate in degrees. */
data class GeoPoint(val lat: Double, val lng: Double) {
    init {
        require(lat in -90.0..90.0) { "latitude out of range: $lat" }
        require(lng in -180.0..180.0) { "longitude out of range: $lng" }
    }

    override fun toString(): String = "GeoPoint(%.6f, %.6f)".format(java.util.Locale.ROOT, lat, lng)
}

/** Mean Earth radius (IUGG), metres. */
const val EARTH_RADIUS_M = 6_371_008.8

internal fun Double.toRadians(): Double = this * PI / 180.0

internal fun Double.toDegrees(): Double = this * 180.0 / PI

object Geo {
    /** Great-circle distance in metres (haversine). Accurate to well under a metre at city scale. */
    fun distance(a: GeoPoint, b: GeoPoint): Double {
        val dLat = (b.lat - a.lat).toRadians()
        val dLng = (b.lng - a.lng).toRadians()
        val h = sin(dLat / 2).let { it * it } +
            cos(a.lat.toRadians()) * cos(b.lat.toRadians()) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Initial bearing from [from] to [to], degrees clockwise from north in 0 until 360. */
    fun bearing(from: GeoPoint, to: GeoPoint): Double {
        val phi1 = from.lat.toRadians()
        val phi2 = to.lat.toRadians()
        val dLng = (to.lng - from.lng).toRadians()
        val y = sin(dLng) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLng)
        return (atan2(y, x).toDegrees() + 360.0) % 360.0
    }

    /** The point [distanceM] metres from [start] along [bearingDeg]. */
    fun destination(
        start: GeoPoint,
        bearingDeg: Double,
        distanceM: Double,
    ): GeoPoint {
        val delta = distanceM / EARTH_RADIUS_M
        val theta = bearingDeg.toRadians()
        val phi1 = start.lat.toRadians()
        val lambda1 = start.lng.toRadians()
        val phi2 = asin(sin(phi1) * cos(delta) + cos(phi1) * sin(delta) * cos(theta))
        val lambda2 = lambda1 + atan2(sin(theta) * sin(delta) * cos(phi1), cos(delta) - sin(phi1) * sin(phi2))
        val lng = ((lambda2.toDegrees() + 540.0) % 360.0) - 180.0
        return GeoPoint(phi2.toDegrees(), lng)
    }

    /** Linear interpolation along the straight segment (fine for the short hops of a walk). */
    fun lerp(
        a: GeoPoint,
        b: GeoPoint,
        t: Double,
    ): GeoPoint = GeoPoint(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
}

/**
 * A local flat projection (equirectangular) around [origin]: x east and y north, in metres.
 * At play-area scale (a few kilometres) its error is far below GPS accuracy, and it makes polygon
 * area, containment and edge distance simple planar geometry.
 */
class LocalFrame(val origin: GeoPoint) {
    private val cosLat = cos(origin.lat.toRadians())
    private val metresPerDegLat = EARTH_RADIUS_M * PI / 180.0
    private val metresPerDegLng = metresPerDegLat * cosLat

    fun toLocal(p: GeoPoint): Vec2 = Vec2((p.lng - origin.lng) * metresPerDegLng, (p.lat - origin.lat) * metresPerDegLat)

    fun toGeo(v: Vec2): GeoPoint = GeoPoint(origin.lat + v.y / metresPerDegLat, origin.lng + v.x / metresPerDegLng)
}

/** A 2D vector in metres. */
data class Vec2(val x: Double, val y: Double) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)

    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)

    operator fun times(k: Double) = Vec2(x * k, y * k)

    infix fun dot(o: Vec2) = x * o.x + y * o.y

    infix fun cross(o: Vec2) = x * o.y - y * o.x

    val length: Double get() = sqrt(x * x + y * y)
}
