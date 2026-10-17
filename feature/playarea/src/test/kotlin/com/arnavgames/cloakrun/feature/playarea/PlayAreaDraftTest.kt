package com.arnavgames.cloakrun.feature.playarea

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.area.PlayAreaCheck
import com.arnavgames.cloakrun.core.game.area.PlayAreaCodec
import com.arnavgames.cloakrun.core.game.area.PlayAreaRules
import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayAreaDraftTest {
    private val rules = PlayAreaRules()
    private val home = GeoPoint(18.5204, 73.8567)

    private fun corner(bearing: Double, distance: Double = 400.0) = Geo.destination(home, bearing, distance)

    @Test
    fun circle_tapMovesTheCentre_andRadiusIsClampedToTheRules() {
        var d = PlayAreaDraft().tap(home, rules)
        assertThat(d.area).isEqualTo(PlayArea.Circle(home, PlayAreaDraft.DEFAULT_RADIUS_M))
        assertThat(d.check(rules)).isEqualTo(PlayAreaCheck.Valid)
        d = d.withRadius(50_000.0, rules)
        assertThat(d.radiusM).isWithin(1e-6).of(rules.maxCircleRadiusM)
        d = d.withRadius(1.0, rules)
        assertThat(d.radiusM).isWithin(1e-6).of(rules.minCircleRadiusM)
    }

    @Test
    fun polygon_buildsFromTaps_andSupportsUndoAndMove() {
        var d = PlayAreaDraft().withShape(AreaShape.Polygon)
        assertThat(d.check(rules)).isEqualTo(PlayAreaCheck.TooFewVertices)
        d = d.tap(corner(0.0), rules).tap(corner(120.0), rules)
        assertThat(d.area).isNull()
        d = d.tap(corner(240.0), rules)
        assertThat(d.area).isInstanceOf(PlayArea.Polygon::class.java)
        assertThat(d.check(rules)).isEqualTo(PlayAreaCheck.Valid)
        d = d.moveVertex(1, corner(120.0, 600.0))
        assertThat(d.vertices[1]).isEqualTo(corner(120.0, 600.0))
        d = d.undo()
        assertThat(d.vertices).hasSize(2)
    }

    @Test
    fun polygon_stopsAcceptingCornersAtTheLimit() {
        var d = PlayAreaDraft().withShape(AreaShape.Polygon)
        repeat(rules.maxVertices + 5) { i -> d = d.tap(corner(i * 360.0 / (rules.maxVertices + 5)), rules) }
        assertThat(d.vertices).hasSize(rules.maxVertices)
    }

    @Test
    fun switchingShape_keepsEachShapesWork() {
        val d = PlayAreaDraft().tap(home, rules).withShape(AreaShape.Polygon).tap(corner(10.0), rules).withShape(AreaShape.Circle)
        assertThat(d.circleCenter).isEqualTo(home)
        assertThat(d.vertices).hasSize(1)
    }

    @Test
    fun codec_roundTripsBothShapes_andRejectsGarbage() {
        val circle = PlayArea.Circle(home, 512.5)
        val polygon = PlayArea.Polygon(listOf(corner(0.0), corner(120.0), corner(240.0)))
        assertThat(PlayAreaCodec.decode(PlayAreaCodec.encode(circle))).isEqualTo(circle)
        assertThat(PlayAreaCodec.decode(PlayAreaCodec.encode(polygon))).isEqualTo(polygon)
        assertThat(PlayAreaCodec.decode("v9|circle|1,2,3")).isNull()
        assertThat(PlayAreaCodec.decode("nonsense")).isNull()
        assertThat(PlayAreaCodec.decode("v1|polygon|1,2;3,4")).isNull()
    }
}
