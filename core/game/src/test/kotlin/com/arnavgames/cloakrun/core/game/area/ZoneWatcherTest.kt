package com.arnavgames.cloakrun.core.game.area

import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ZoneWatcherTest {
    private val center = GeoPoint(22.5726, 88.3639)
    private val area = PlayArea.Circle(center, 300.0)

    private fun fixAt(distanceFromCenter: Double, accuracy: Float = 8f) =
        LocationFix(Geo.destination(center, 45.0, distanceFromCenter), accuracy, 0L)

    @Test
    fun walkingOut_reportsNearEdgeThenOutside_andBackIn() {
        val watcher = ZoneWatcher(area)
        assertThat(watcher.onFix(fixAt(100.0))).isNull()
        assertThat(watcher.status).isEqualTo(ZoneStatus.Inside)
        assertThat(watcher.onFix(fixAt(280.0))?.to).isEqualTo(ZoneStatus.NearEdge)
        assertThat(watcher.onFix(fixAt(320.0))?.to).isEqualTo(ZoneStatus.Outside)
        assertThat(watcher.onFix(fixAt(270.0))?.to).isEqualTo(ZoneStatus.NearEdge)
        assertThat(watcher.onFix(fixAt(150.0))?.to).isEqualTo(ZoneStatus.Inside)
    }

    @Test
    fun jitterAcrossTheEdge_doesNotFlicker() {
        val watcher = ZoneWatcher(area)
        watcher.onFix(fixAt(270.0))
        assertThat(watcher.status).isEqualTo(ZoneStatus.NearEdge)
        // Bouncing a metre either side of the boundary with 8 m accuracy stays NearEdge.
        repeat(10) { i -> assertThat(watcher.onFix(fixAt(if (i % 2 == 0) 301.0 else 299.0))).isNull() }
        assertThat(watcher.status).isEqualTo(ZoneStatus.NearEdge)
    }

    @Test
    fun inaccurateFixes_areIgnored() {
        val watcher = ZoneWatcher(area)
        watcher.onFix(fixAt(100.0))
        assertThat(watcher.onFix(fixAt(500.0, accuracy = 120f))).isNull()
        assertThat(watcher.status).isEqualTo(ZoneStatus.Inside)
    }

    @Test
    fun firstFix_classifiesDirectly() {
        val watcher = ZoneWatcher(area)
        assertThat(watcher.onFix(fixAt(600.0))?.to).isEqualTo(ZoneStatus.Outside)
    }
}
