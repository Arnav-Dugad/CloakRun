package com.arnavgames.cloakrun.core.game.integrity

import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.geo.LocalFrame
import com.arnavgames.cloakrun.core.game.geo.Vec2
import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.game.sim.BotWalker
import com.arnavgames.cloakrun.core.game.sim.RoadGraph
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

class RoutePlausibilityTest {
    private val origin = GeoPoint(40.7829, -73.9654)
    private val frame = LocalFrame(origin)

    private fun at(x: Double, y: Double) = frame.toGeo(Vec2(x, y))

    /** A 5 x 5 grid of streets with 100 m blocks. */
    private val graph: RoadGraph = RoadGraph.build(
        (0..4).flatMap { i ->
            listOf(
                (0..4).map { j -> at(i * 100.0, j * 100.0) },
                (0..4).map { j -> at(j * 100.0, i * 100.0) },
            )
        },
    )

    /** A building filling the block between the streets at x 0..100, y 0..100 (with a 10 m pavement). */
    private val building = listOf(at(10.0, 10.0), at(90.0, 10.0), at(90.0, 90.0), at(10.0, 90.0))

    private fun fix(
        x: Double,
        y: Double,
        t: Long,
        accuracy: Float = 6f,
        mock: Boolean = false,
    ) = LocationFix(at(x, y), accuracy, t, isMock = mock)

    private val checker = RoutePlausibility(graph, listOf(building), origin)

    @Test
    fun honestWalkAlongStreets_isClean() {
        val walker = BotWalker(graph, Random(5), startNode = 0)
        val trail = (0 until 600 step 3).map { s ->
            walker.advance(3.0)
            LocationFix(walker.position, 6f, s * 1_000L)
        }
        val report = checker.evaluate(trail)
        assertThat(report.events).isEmpty()
        assertThat(report.verdict).isEqualTo(IntegrityVerdict.Clean)
    }

    @Test
    fun mockProvider_isFlagged() {
        val report = checker.evaluate(listOf(fix(0.0, 0.0, 0), fix(0.0, 5.0, 3_000, mock = true)))
        assertThat(report.flags).contains(IntegrityFlag.MockLocation)
        assertThat(report.verdict).isEqualTo(IntegrityVerdict.LikelySpoofed)
    }

    @Test
    fun teleport_isFlagged() {
        val report = checker.evaluate(listOf(fix(0.0, 0.0, 0), fix(0.0, 400.0, 3_000)))
        assertThat(report.flags).contains(IntegrityFlag.Teleport)
    }

    @Test
    fun sustainedDrivingSpeed_isFlagged_butASprintIsNot() {
        val driving = (0..6).map { i -> fix(0.0, i * 60.0, i * 4_000L) } // 15 m/s
        assertThat(checker.evaluate(driving).flags).contains(IntegrityFlag.ImpossibleSpeed)
        val sprint = (0..6).map { i -> fix(0.0, i * 28.0, i * 4_000L) } // 7 m/s
        assertThat(checker.evaluate(sprint).flags).doesNotContain(IntegrityFlag.ImpossibleSpeed)
    }

    @Test
    fun longTimeFarFromStreets_isFlagged() {
        // Standing 80 m outside the grid for two minutes.
        val trail = (0..40).map { i -> fix(-80.0, 200.0, i * 3_000L) }
        assertThat(checker.evaluate(trail).flags).contains(IntegrityFlag.OffNetwork)
    }

    @Test
    fun straightRunThroughABuilding_isFlagged() {
        val trail = listOf(fix(0.0, 50.0, 0), fix(100.0, 50.0, 30_000))
        val report = checker.evaluate(trail)
        assertThat(report.flags).contains(IntegrityFlag.ThroughBuildings)
        // One building crossing on its own is a soft signal: it lowers the score but does not condemn.
        assertThat(report.score).isEqualTo(85)
        assertThat(report.verdict).isEqualTo(IntegrityVerdict.Clean)
    }

    @Test
    fun poorAccuracyFixes_areNotJudged() {
        val trail = listOf(fix(0.0, 0.0, 0, accuracy = 80f), fix(0.0, 900.0, 2_000, accuracy = 80f))
        assertThat(checker.evaluate(trail).events).isEmpty()
    }
}
