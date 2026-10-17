package com.arnavgames.cloakrun.core.game.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoTest {
    private val indiaGate = GeoPoint(28.612894, 77.229446)
    private val rashtrapatiBhavan = GeoPoint(28.614369, 77.199554)

    @Test
    fun distance_matchesAKnownCityScaleMeasurement() {
        // About 2.92 km along Kartavya Path (haversine with the IUGG mean radius).
        assertThat(Geo.distance(indiaGate, rashtrapatiBhavan)).isWithin(15.0).of(2_921.0)
    }

    @Test
    fun distance_isSymmetricAndZeroToItself() {
        assertThat(Geo.distance(indiaGate, rashtrapatiBhavan)).isWithin(1e-6).of(Geo.distance(rashtrapatiBhavan, indiaGate))
        assertThat(Geo.distance(indiaGate, indiaGate)).isWithin(1e-9).of(0.0)
    }

    @Test
    fun destination_roundTripsWithDistanceAndBearing() {
        for (bearing in listOf(0.0, 45.0, 90.0, 180.0, 271.0)) {
            val d = Geo.destination(indiaGate, bearing, 750.0)
            assertThat(Geo.distance(indiaGate, d)).isWithin(0.01).of(750.0)
            assertThat(Geo.bearing(indiaGate, d)).isWithin(0.05).of(bearing)
        }
    }

    @Test
    fun localFrame_roundTripsAndPreservesDistance() {
        val frame = LocalFrame(indiaGate)
        val v = frame.toLocal(rashtrapatiBhavan)
        assertThat(v.length).isWithin(5.0).of(Geo.distance(indiaGate, rashtrapatiBhavan))
        val back = frame.toGeo(v)
        assertThat(Geo.distance(back, rashtrapatiBhavan)).isLessThan(0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidLatitude_isRejected() {
        GeoPoint(91.0, 0.0)
    }
}
