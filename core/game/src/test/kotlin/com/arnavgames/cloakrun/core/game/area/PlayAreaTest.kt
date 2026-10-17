package com.arnavgames.cloakrun.core.game.area

import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.google.common.truth.Truth.assertThat
import kotlin.math.PI
import org.junit.Test

class PlayAreaTest {
    private val center = GeoPoint(12.9716, 77.5946)
    private val rules = PlayAreaRules()

    private fun squareOfSide(sideM: Double): PlayArea.Polygon {
        val half = sideM / 2
        val north = Geo.destination(center, 0.0, half)
        val south = Geo.destination(center, 180.0, half)
        return PlayArea.Polygon(
            listOf(
                Geo.destination(north, 270.0, half),
                Geo.destination(north, 90.0, half),
                Geo.destination(south, 90.0, half),
                Geo.destination(south, 270.0, half),
            ),
        )
    }

    @Test
    fun circle_areaContainmentAndEdgeDistance() {
        val circle = PlayArea.Circle(center, 500.0)
        assertThat(circle.areaM2).isWithin(1.0).of(PI * 500 * 500)
        val inside = Geo.destination(center, 30.0, 400.0)
        val outside = Geo.destination(center, 30.0, 650.0)
        assertThat(circle.contains(inside)).isTrue()
        assertThat(circle.contains(outside)).isFalse()
        assertThat(circle.signedDistanceToEdge(inside)).isWithin(0.5).of(100.0)
        assertThat(circle.signedDistanceToEdge(outside)).isWithin(0.5).of(-150.0)
    }

    @Test
    fun polygon_areaMatchesGeodesicSquare() {
        val square = squareOfSide(1_000.0)
        assertThat(square.areaM2).isWithin(2_000.0).of(1_000_000.0)
        assertThat(square.contains(center)).isTrue()
        assertThat(square.signedDistanceToEdge(center)).isWithin(2.0).of(500.0)
    }

    @Test
    fun validation_enforcesSizeLimitsAndShape() {
        assertThat(rules.validate(PlayArea.Circle(center, 50.0))).isEqualTo(PlayAreaCheck.TooSmall)
        assertThat(rules.validate(PlayArea.Circle(center, 500.0))).isEqualTo(PlayAreaCheck.Valid)
        assertThat(rules.validate(PlayArea.Circle(center, 1_200.0))).isEqualTo(PlayAreaCheck.TooLarge)
        assertThat(rules.validate(squareOfSide(2_000.0))).isEqualTo(PlayAreaCheck.TooLarge)
        val v = squareOfSide(600.0).vertices
        val bowTie = PlayArea.Polygon(listOf(v[0], v[2], v[1], v[3]))
        assertThat(rules.validate(bowTie)).isEqualTo(PlayAreaCheck.SelfIntersecting)
    }

    @Test
    fun maxCircleRadius_isConsistentWithMaxArea() {
        val atLimit = PlayArea.Circle(center, rules.maxCircleRadiusM - 0.5)
        assertThat(rules.validate(atLimit)).isEqualTo(PlayAreaCheck.Valid)
    }

    @Test
    fun areaReadout_picksAReadableUnit() {
        assertThat(formatArea(5_000.0)).isEqualTo(AreaReadout(5_000.0, AreaUnit.SquareMetres))
        assertThat(formatArea(250_000.0)).isEqualTo(AreaReadout(25.0, AreaUnit.Hectares))
        assertThat(formatArea(2_500_000.0)).isEqualTo(AreaReadout(2.5, AreaUnit.SquareKilometres))
    }
}
