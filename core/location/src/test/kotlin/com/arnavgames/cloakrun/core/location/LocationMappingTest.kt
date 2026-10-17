package com.arnavgames.cloakrun.core.location

import android.location.Location
import com.arnavgames.cloakrun.core.game.location.FixSource
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationMappingTest {
    private fun location(mock: Boolean) = Location("fused").apply {
        latitude = 12.9763
        longitude = 77.5929
        accuracy = 7.5f
        time = 1_700_000_000_000L
        speed = 1.4f
        bearing = 90f
        isMock = mock
    }

    @Test
    fun deviceLocation_mapsEveryField() {
        val fix = location(mock = false).toFix()
        assertThat(fix.point.lat).isWithin(1e-9).of(12.9763)
        assertThat(fix.point.lng).isWithin(1e-9).of(77.5929)
        assertThat(fix.accuracyM).isEqualTo(7.5f)
        assertThat(fix.timeMillis).isEqualTo(1_700_000_000_000L)
        assertThat(fix.speedMps).isEqualTo(1.4f)
        assertThat(fix.bearingDeg).isEqualTo(90f)
        assertThat(fix.isMock).isFalse()
        assertThat(fix.source).isEqualTo(FixSource.Device)
    }

    @Test
    fun mockProvider_isFlagged() {
        assertThat(location(mock = true).toFix().isMock).isTrue()
    }

    @Test
    fun missingAccuracy_isTreatedAsUnknownNotPerfect() {
        val fix = Location("fused").apply {
            latitude = 1.0
            longitude = 1.0
        }.toFix()
        assertThat(fix.accuracyM).isEqualTo(Float.MAX_VALUE)
        assertThat(fix.speedMps).isNull()
    }
}
