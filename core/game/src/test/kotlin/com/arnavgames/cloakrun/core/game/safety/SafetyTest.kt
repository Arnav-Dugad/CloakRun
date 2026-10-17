package com.arnavgames.cloakrun.core.game.safety

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.area.PlayAreaRules
import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.geo.LocalFrame
import com.arnavgames.cloakrun.core.game.geo.Vec2
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SafetyTest {
    private val origin = GeoPoint(51.5079, -0.1281)
    private val frame = LocalFrame(origin)
    private val rules = PlayAreaRules()

    /** A rectangle in local metres, as a geographic ring. */
    private fun rect(
        x0: Double,
        y0: Double,
        x1: Double,
        y1: Double,
    ): List<GeoPoint> = listOf(Vec2(x0, y0), Vec2(x1, y0), Vec2(x1, y1), Vec2(x0, y1)).map(frame::toGeo)

    private fun at(x: Double, y: Double) = frame.toGeo(Vec2(x, y))

    @Test
    fun snap_aSimplePark_returnsItsBoundary() {
        val park = rect(-200.0, -150.0, 200.0, 150.0)
        val result = ParkSnap.snap(at(0.0, 0.0), listOf(park), emptyList(), rules)
        assertThat(result).isInstanceOf(SnapResult.Snapped::class.java)
        val area = (result as SnapResult.Snapped).area
        assertThat(area.areaM2).isWithin(400.0 * 300.0 * 0.06).of(400.0 * 300.0)
        assertThat(area.vertices.size).isIn(4..rules.maxVertices)
        assertThat(area.isSelfIntersecting).isFalse()
        assertThat(result.cutAtHazards).isFalse()
    }

    @Test
    fun snap_mergesTileClippedPieces() {
        // The same park delivered as two tiles that overlap slightly at x = 0.
        val left = rect(-200.0, -150.0, 5.0, 150.0)
        val right = rect(-5.0, -150.0, 200.0, 150.0)
        val area = (ParkSnap.snap(at(-100.0, 0.0), listOf(left, right), emptyList(), rules) as SnapResult.Snapped).area
        assertThat(area.areaM2).isWithin(400.0 * 300.0 * 0.06).of(400.0 * 300.0)
        assertThat(area.contains(at(150.0, 100.0))).isTrue()
    }

    @Test
    fun snap_stopsAtAMajorRoadThroughThePark() {
        val park = rect(-200.0, -150.0, 200.0, 150.0)
        val road = Hazard.Corridor(HazardKind.MajorRoad, listOf(at(0.0, -400.0), at(0.0, 400.0)), 10.0)
        val result = ParkSnap.snap(at(-100.0, 0.0), listOf(park), listOf(road), rules) as SnapResult.Snapped
        assertThat(result.cutAtHazards).isTrue()
        assertThat(result.area.contains(at(-150.0, 0.0))).isTrue()
        assertThat(result.area.contains(at(100.0, 0.0))).isFalse()
        assertThat(result.area.areaM2).isWithin(190.0 * 300.0 * 0.08).of(190.0 * 300.0)
    }

    @Test
    fun snap_fillsHolesSoPondsBecomeNoGoZonesNotHoles() {
        val park = rect(-200.0, -150.0, 200.0, 150.0)
        val pond = Hazard.Area(HazardKind.Water, rect(-30.0, -30.0, 30.0, 30.0))
        val result = ParkSnap.snap(at(-120.0, 0.0), listOf(park), listOf(pond), rules) as SnapResult.Snapped
        // The outline is the whole park; the pond is handled as a hatched no-go zone instead.
        assertThat(result.area.contains(at(0.0, 0.0))).isTrue()
        assertThat(HazardMap(listOf(pond), origin).isSafe(at(0.0, 0.0))).isFalse()
    }

    @Test
    fun snap_reportsNotAParkAndTooSmall() {
        val park = rect(-200.0, -150.0, 200.0, 150.0)
        assertThat(ParkSnap.snap(at(500.0, 500.0), listOf(park), emptyList(), rules)).isEqualTo(SnapResult.NotAPark)
        assertThat(ParkSnap.snap(at(0.0, 0.0), emptyList(), emptyList(), rules)).isEqualTo(SnapResult.NotAPark)
        val tiny = rect(-20.0, -20.0, 20.0, 20.0)
        assertThat(ParkSnap.snap(at(0.0, 0.0), listOf(tiny), emptyList(), rules)).isEqualTo(SnapResult.TooSmall)
    }

    @Test
    fun snap_complexShape_isSimplifiedWithinTheCornerLimit() {
        // A serrated park edge with 60 teeth must still produce at most 24 corners.
        val teeth = (0..60).flatMap { i ->
            val x = -300.0 + i * 10.0
            listOf(Vec2(x, 150.0 + (if (i % 2 == 0) 0.0 else 12.0)))
        }
        val ring = (listOf(Vec2(-300.0, -150.0), Vec2(300.0, -150.0)) + teeth.reversed()).map(frame::toGeo)
        val area = (ParkSnap.snap(at(0.0, 0.0), listOf(ring), emptyList(), rules) as SnapResult.Snapped).area
        assertThat(area.vertices.size).isAtMost(rules.maxVertices)
        assertThat(area.isSelfIntersecting).isFalse()
    }

    @Test
    fun hazardMap_findsAreasAndCorridors() {
        val road = Hazard.Corridor(HazardKind.MajorRoad, listOf(at(-500.0, 0.0), at(500.0, 0.0)), 12.0)
        val water = Hazard.Area(HazardKind.Water, rect(100.0, 100.0, 200.0, 200.0))
        val map = HazardMap(listOf(road, water), origin)
        assertThat(map.hazardAt(at(0.0, 5.0))).isEqualTo(road)
        assertThat(map.hazardAt(at(0.0, 30.0))).isNull()
        assertThat(map.hazardAt(at(150.0, 150.0))).isEqualTo(water)
    }

    @Test
    fun summary_countsTouchingHazards_andEstimatesCoverage() {
        val area = PlayArea.Circle(origin, 300.0)
        val far = Hazard.Area(HazardKind.Water, rect(2_000.0, 2_000.0, 2_100.0, 2_100.0))
        val half = Hazard.Area(HazardKind.Restricted, rect(0.0, -400.0, 400.0, 400.0))
        val summary = HazardMap(listOf(far, half), origin).summarise(area)
        assertThat(summary.hazards).containsExactly(half)
        assertThat(summary.countByKind).containsExactly(HazardKind.Restricted, 1)
        assertThat(summary.coveredFraction).isWithin(0.06).of(0.5)
        assertThat(summary.isMostlyUnsafe).isTrue()
        assertThat(Geo.distance(origin, area.center)).isLessThan(1.0)
    }
}
