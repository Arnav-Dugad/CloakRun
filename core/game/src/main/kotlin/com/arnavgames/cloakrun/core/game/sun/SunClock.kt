package com.arnavgames.cloakrun.core.game.sun

import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

/** Sunrise and sunset for a day at a place. Null fields mean the sun never rises or never sets. */
data class SunTimes(
    val riseMillis: Long?,
    val setMillis: Long?,
    val polarDay: Boolean,
    val polarNight: Boolean,
)

/**
 * Sunrise and sunset from the standard low-precision solar position formulas (as used by the
 * widely known SunCalc). Accurate to about a minute, which is plenty to switch the map between
 * its day and night styles at local sunset.
 */
object SunClock {
    private const val DAY_MS = 86_400_000.0
    private const val J1970 = 2_440_588.0
    private const val J2000 = 2_451_545.0
    private const val J0 = 0.0009
    private const val RAD = PI / 180
    private const val OBLIQUITY = RAD * 23.4397

    /** Standard sunset altitude: the upper limb touching the horizon, with refraction. */
    private const val SUNSET_ALTITUDE = -0.833 * RAD

    fun times(dayMillis: Long, at: GeoPoint): SunTimes {
        val lw = RAD * -at.lng
        val phi = RAD * at.lat
        val d = toJulian(dayMillis) - J2000
        val n = (d - J0 - lw / (2 * PI)).roundToLong().toDouble()
        val ds = J0 + lw / (2 * PI) + n
        val m = RAD * (357.5291 + 0.98560028 * ds)
        val c = RAD * (1.9148 * sin(m) + 0.02 * sin(2 * m) + 0.0003 * sin(3 * m))
        val l = m + c + RAD * 102.9372 + PI
        val dec = asin(sin(OBLIQUITY) * sin(l))
        val noon = J2000 + ds + 0.0053 * sin(m) - 0.0069 * sin(2 * l)

        val cosW = (sin(SUNSET_ALTITUDE) - sin(phi) * sin(dec)) / (cos(phi) * cos(dec))
        if (cosW < -1) return SunTimes(null, null, polarDay = true, polarNight = false)
        if (cosW > 1) return SunTimes(null, null, polarDay = false, polarNight = true)
        val w = acos(cosW)
        val setApprox = J0 + (w + lw) / (2 * PI) + n
        val set = J2000 + setApprox + 0.0053 * sin(m) - 0.0069 * sin(2 * l)
        val rise = noon - (set - noon)
        return SunTimes(fromJulian(rise), fromJulian(set), polarDay = false, polarNight = false)
    }

    /** True between local sunset and sunrise. */
    fun isNight(nowMillis: Long, at: GeoPoint): Boolean {
        val t = times(nowMillis, at)
        return when {
            t.polarDay -> false
            t.polarNight -> true
            else -> nowMillis < t.riseMillis!! || nowMillis >= t.setMillis!!
        }
    }

    private fun toJulian(millis: Long): Double = millis / DAY_MS - 0.5 + J1970

    private fun fromJulian(j: Double): Long = ((j + 0.5 - J1970) * DAY_MS).roundToLong()
}
