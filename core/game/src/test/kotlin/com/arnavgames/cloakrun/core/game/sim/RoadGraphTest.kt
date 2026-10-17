package com.arnavgames.cloakrun.core.game.sim

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

class RoadGraphTest {
    private val origin = GeoPoint(19.0760, 72.8777)

    /** A 4 x 4 street grid with 100 m blocks, drawn as separate features that meet at junctions. */
    private fun grid(): List<List<GeoPoint>> {
        val lines = mutableListOf<List<GeoPoint>>()
        for (i in 0..3) {
            val rowStart = Geo.destination(origin, 180.0, i * 100.0)
            lines += (0..3).map { Geo.destination(rowStart, 90.0, it * 100.0) }
            // Columns are offset by 1.5 m, as real features rarely share exact coordinates.
            val colStart = Geo.destination(origin, 90.0, i * 100.0)
            lines += (0..3).map { Geo.destination(colStart, 180.0, it * 100.0 + 1.5) }
        }
        return lines
    }

    @Test
    fun build_snapsJunctionsTogether() {
        val graph = RoadGraph.build(grid(), snapM = 4.0)
        assertThat(graph.nodes).hasSize(16)
        assertThat(graph.edgeCount).isEqualTo(24)
        assertThat(graph.largestComponent()).hasSize(16)
    }

    @Test
    fun walker_staysOnStreetsAtWalkingPace() {
        val graph = RoadGraph.build(grid())
        val bot = BotWalker(graph, Random(7), startNode = 0)
        var last = bot.position
        var walked = 0.0
        repeat(600) {
            bot.advance(1.0)
            walked += Geo.distance(last, bot.position)
            last = bot.position
            val nearestJunction = graph.nodes.minOf { Geo.distance(it, bot.position) }
            // On a 100 m grid, a point on a street is never more than 50 m from a junction.
            assertThat(nearestJunction).isAtMost(51.0)
        }
        // Ten minutes at about 1.4 m/s with occasional pauses.
        assertThat(walked).isAtLeast(400.0)
        assertThat(walked).isAtMost(1_000.0)
    }

    @Test
    fun walker_isDeterministicForASeed() {
        val graph = RoadGraph.build(grid())
        val a = BotWalker(graph, Random(3), 5)
        val b = BotWalker(graph, Random(3), 5)
        repeat(300) {
            a.advance(0.5)
            b.advance(0.5)
        }
        assertThat(a.position).isEqualTo(b.position)
    }

    @Test
    fun walker_prefersToStayInsideTheAllowedArea() {
        val graph = RoadGraph.build(grid())
        val area = PlayArea.Circle(Geo.destination(Geo.destination(origin, 90.0, 100.0), 180.0, 100.0), 160.0)
        val bot = BotWalker(graph, Random(11), graph.nearestNode(area.center), allowed = area::contains)
        var outsideSteps = 0
        repeat(1_200) {
            bot.advance(1.0)
            if (!area.contains(bot.position)) outsideSteps++
        }
        assertThat(outsideSteps).isLessThan(60)
    }
}
