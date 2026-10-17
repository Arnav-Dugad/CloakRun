package com.arnavgames.cloakrun.core.game.sun

import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Test

class SunClockTest {
    private fun utc(text: String): Long = Instant.parse(text).toEpochMilli()

    private fun minutesBetween(a: Long, b: Long): Double = kotlin.math.abs(a - b) / 60_000.0

    @Test
    fun london_midsummer() {
        // Reference (timeanddate.com, 2026-06-21 London): sunrise 03:43 UTC, sunset 20:21 UTC.
        val t = SunClock.times(utc("2026-06-21T12:00:00Z"), GeoPoint(51.5074, -0.1278))
        assertThat(minutesBetween(t.riseMillis!!, utc("2026-06-21T03:43:00Z"))).isLessThan(4.0)
        assertThat(minutesBetween(t.setMillis!!, utc("2026-06-21T20:21:00Z"))).isLessThan(4.0)
    }

    @Test
    fun bengaluru_october() {
        // Bengaluru in mid October: sunrise about 06:08 IST, sunset about 17:58 IST.
        val istNoon = ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)).toInstant().toEpochMilli()
        val t = SunClock.times(istNoon, GeoPoint(12.9716, 77.5946))
        val rise = Instant.ofEpochMilli(t.riseMillis!!).atOffset(ZoneOffset.ofHoursMinutes(5, 30))
        val set = Instant.ofEpochMilli(t.setMillis!!).atOffset(ZoneOffset.ofHoursMinutes(5, 30))
        assertThat(rise.hour * 60 + rise.minute).isIn((6 * 60)..(6 * 60 + 15))
        assertThat(set.hour * 60 + set.minute).isIn((17 * 60 + 50)..(18 * 60 + 5))
    }

    @Test
    fun isNight_flipsAtSunsetAndSunrise() {
        val place = GeoPoint(51.5074, -0.1278)
        assertThat(SunClock.isNight(utc("2026-06-21T12:00:00Z"), place)).isFalse()
        assertThat(SunClock.isNight(utc("2026-06-21T22:30:00Z"), place)).isTrue()
        assertThat(SunClock.isNight(utc("2026-06-21T02:30:00Z"), place)).isTrue()
    }

    @Test
    fun polarDayAndNight_areReported() {
        val tromso = GeoPoint(69.6492, 18.9553)
        assertThat(SunClock.times(utc("2026-06-21T12:00:00Z"), tromso).polarDay).isTrue()
        assertThat(SunClock.times(utc("2026-12-21T12:00:00Z"), tromso).polarNight).isTrue()
        assertThat(SunClock.isNight(utc("2026-12-21T12:00:00Z"), tromso)).isTrue()
    }
}
