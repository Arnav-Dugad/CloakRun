package com.arnavgames.cloakrun.core.game.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlanarTest {
    private val square = listOf(Vec2(0.0, 0.0), Vec2(100.0, 0.0), Vec2(100.0, 100.0), Vec2(0.0, 100.0))
    private val bowTie = listOf(Vec2(0.0, 0.0), Vec2(100.0, 100.0), Vec2(100.0, 0.0), Vec2(0.0, 100.0))
    private val lShape = listOf(
        Vec2(0.0, 0.0),
        Vec2(60.0, 0.0),
        Vec2(60.0, 20.0),
        Vec2(20.0, 20.0),
        Vec2(20.0, 60.0),
        Vec2(0.0, 60.0),
    )

    @Test
    fun area_ofSquareAndConcaveShape() {
        assertThat(Planar.area(square)).isWithin(1e-9).of(10_000.0)
        assertThat(Planar.area(lShape)).isWithin(1e-9).of(60.0 * 20 + 20.0 * 40)
    }

    @Test
    fun contains_handlesConcaveNotch() {
        assertThat(Planar.contains(lShape, Vec2(10.0, 50.0))).isTrue()
        assertThat(Planar.contains(lShape, Vec2(40.0, 40.0))).isFalse()
        assertThat(Planar.contains(square, Vec2(100.0, 50.0))).isTrue()
        assertThat(Planar.contains(square, Vec2(101.0, 50.0))).isFalse()
    }

    @Test
    fun distanceToBoundary_isPerpendicularDistance() {
        assertThat(Planar.distanceToBoundary(square, Vec2(50.0, 30.0))).isWithin(1e-9).of(30.0)
        assertThat(Planar.distanceToBoundary(square, Vec2(130.0, 50.0))).isWithin(1e-9).of(30.0)
    }

    @Test
    fun selfIntersection_isDetected() {
        assertThat(Planar.isSelfIntersecting(bowTie)).isTrue()
        assertThat(Planar.isSelfIntersecting(square)).isFalse()
        assertThat(Planar.isSelfIntersecting(lShape)).isFalse()
    }
}
