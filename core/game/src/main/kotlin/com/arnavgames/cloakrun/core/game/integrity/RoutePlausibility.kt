package com.arnavgames.cloakrun.core.game.integrity

import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.geo.LocalFrame
import com.arnavgames.cloakrun.core.game.geo.Planar
import com.arnavgames.cloakrun.core.game.geo.Vec2
import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.game.sim.RoadGraph
import kotlin.math.floor

/** Something about a trail that a real person on foot would not do. */
enum class IntegrityFlag {
    /** Android reported a mock location provider. */
    MockLocation,

    /** Sustained speed beyond a sprint. */
    ImpossibleSpeed,

    /** A jump far larger than GPS error allows in a few seconds. */
    Teleport,

    /** A long time far from any street or path. */
    OffNetwork,

    /** A straight run through building footprints. */
    ThroughBuildings,
}

data class IntegrityEvent(
    val flag: IntegrityFlag,
    val atMillis: Long,
    val detail: String,
)

enum class IntegrityVerdict { Clean, Suspicious, LikelySpoofed }

/** The on-device integrity report for one player's trail. Flags are surfaced to the host, never auto-banned. */
data class IntegrityReport(val events: List<IntegrityEvent>, val score: Int) {
    val verdict: IntegrityVerdict
        get() = when {
            score >= CLEAN_SCORE -> IntegrityVerdict.Clean
            score >= SUSPICIOUS_SCORE -> IntegrityVerdict.Suspicious
            else -> IntegrityVerdict.LikelySpoofed
        }

    val flags: Set<IntegrityFlag> get() = events.map { it.flag }.toSet()

    companion object {
        const val CLEAN_SCORE = 85
        const val SUSPICIOUS_SCORE = 50
        val Clean = IntegrityReport(emptyList(), 100)
    }
}

/**
 * Checks whether a trail of fixes is plausible for someone on foot in the play area. Cheap and
 * fully local; complements Bluetooth proximity proofs for tags.
 *
 * Only fixes with usable accuracy are judged, and every threshold leaves room for GPS error, so
 * an honest player in poor reception is not flagged.
 */
class RoutePlausibility(
    graph: RoadGraph? = null,
    buildings: List<List<GeoPoint>> = emptyList(),
    origin: GeoPoint? = null,
    private val config: Config = Config(),
) {
    data class Config(
        val maxAccuracyM: Float = 25f,
        val sprintMps: Double = 8.5,
        val absurdMps: Double = 14.0,
        val teleportM: Double = 120.0,
        val teleportWindowMs: Long = 5_000,
        val offNetworkM: Double = 45.0,
        val offNetworkMs: Long = 60_000,
        val buildingRunM: Double = 25.0,
    )

    private val frame: LocalFrame? = origin?.let(::LocalFrame) ?: graph?.nodes?.firstOrNull()?.let(::LocalFrame)
    private val network: SegmentIndex? = graph?.let { g -> frame?.let { SegmentIndex(g, it) } }
    private val buildingRings: List<List<Vec2>> = frame?.let { f -> buildings.map { r -> r.map(f::toLocal) } }.orEmpty()

    fun evaluate(trail: List<LocationFix>): IntegrityReport {
        val events = mutableListOf<IntegrityEvent>()
        trail.firstOrNull { it.isMock }?.let { events += IntegrityEvent(IntegrityFlag.MockLocation, it.timeMillis, "mock provider") }
        val good = trail.filter { it.accuracyM <= config.maxAccuracyM }.sortedBy { it.timeMillis }
        checkSpeed(good, events)
        checkNetwork(good, events)
        checkBuildings(good, events)
        return IntegrityReport(events, score(events))
    }

    private fun checkSpeed(fixes: List<LocationFix>, events: MutableList<IntegrityEvent>) {
        var fastStreak = 0
        for (i in 1 until fixes.size) {
            val a = fixes[i - 1]
            val b = fixes[i]
            val dt = (b.timeMillis - a.timeMillis).coerceAtLeast(1)
            val slack = (a.accuracyM + b.accuracyM).toDouble()
            val d = (Geo.distance(a.point, b.point) - slack).coerceAtLeast(0.0)
            val speed = d / (dt / 1_000.0)
            if (dt <= config.teleportWindowMs && d >= config.teleportM) {
                events += IntegrityEvent(IntegrityFlag.Teleport, b.timeMillis, "%.0f m in %.1f s".format(d, dt / 1_000.0))
            } else if (speed > config.absurdMps) {
                events += IntegrityEvent(IntegrityFlag.ImpossibleSpeed, b.timeMillis, "%.1f m/s".format(speed))
            }
            fastStreak = if (speed > config.sprintMps) fastStreak + 1 else 0
            if (fastStreak == 3) events += IntegrityEvent(IntegrityFlag.ImpossibleSpeed, b.timeMillis, "sustained %.1f m/s".format(speed))
        }
    }

    private fun checkNetwork(fixes: List<LocationFix>, events: MutableList<IntegrityEvent>) {
        val index = network ?: return
        val f = frame ?: return
        var offSince: Long? = null
        var reported = false
        for (fix in fixes) {
            val far = index.distanceTo(f.toLocal(fix.point)) > config.offNetworkM + fix.accuracyM
            if (!far) {
                offSince = null
                reported = false
                continue
            }
            val since = offSince ?: fix.timeMillis.also { offSince = it }
            if (!reported && fix.timeMillis - since >= config.offNetworkMs) {
                events += IntegrityEvent(IntegrityFlag.OffNetwork, fix.timeMillis, "off streets for ${(fix.timeMillis - since) / 1_000} s")
                reported = true
            }
        }
    }

    private fun checkBuildings(fixes: List<LocationFix>, events: MutableList<IntegrityEvent>) {
        val f = frame ?: return
        if (buildingRings.isEmpty()) return
        for (i in 1 until fixes.size) {
            val a = f.toLocal(fixes[i - 1].point)
            val b = f.toLocal(fixes[i].point)
            val length = (b - a).length
            if (length < config.buildingRunM) continue
            val steps = (length / SAMPLE_STEP_M).toInt().coerceAtLeast(1)
            var inside = 0
            for (s in 0..steps) {
                val p = a + (b - a) * (s.toDouble() / steps)
                if (buildingRings.any { Planar.contains(it, p) }) inside++
            }
            val insideMetres = inside * length / (steps + 1)
            if (insideMetres >= config.buildingRunM) {
                events +=
                    IntegrityEvent(IntegrityFlag.ThroughBuildings, fixes[i].timeMillis, "%.0f m through buildings".format(insideMetres))
            }
        }
    }

    private fun score(events: List<IntegrityEvent>): Int {
        val penalty = events.sumOf {
            when (it.flag) {
                IntegrityFlag.MockLocation -> 60
                IntegrityFlag.Teleport -> 35
                IntegrityFlag.ImpossibleSpeed -> 20
                IntegrityFlag.ThroughBuildings -> 15
                IntegrityFlag.OffNetwork -> 10
            }
        }
        return (100 - penalty).coerceIn(0, 100)
    }

    /** Street and path segments bucketed on a grid for fast nearest-distance queries. */
    private class SegmentIndex(graph: RoadGraph, frame: LocalFrame) {
        private val segments = mutableListOf<Pair<Vec2, Vec2>>()
        private val buckets = HashMap<Long, MutableList<Int>>()

        init {
            for (a in graph.nodes.indices) {
                for (b in graph.neighbours(a)) {
                    if (b > a) segments += frame.toLocal(graph.nodes[a]) to frame.toLocal(graph.nodes[b])
                }
            }
            segments.forEachIndexed { i, (a, b) ->
                for (cx in cell(minOf(a.x, b.x))..cell(maxOf(a.x, b.x))) {
                    for (cy in cell(minOf(a.y, b.y))..cell(maxOf(a.y, b.y))) buckets.getOrPut(key(cx, cy)) { mutableListOf() } += i
                }
            }
        }

        fun distanceTo(p: Vec2): Double {
            var best = Double.MAX_VALUE
            val cx = cell(p.x)
            val cy = cell(p.y)
            for (dx in -2..2) {
                for (dy in -2..2) {
                    buckets[key(cx + dx, cy + dy)]?.forEach { i ->
                        val (a, b) = segments[i]
                        best = minOf(best, Planar.distanceToSegment(p, a, b))
                    }
                }
            }
            return best
        }

        private fun cell(v: Double) = floor(v / BUCKET_M).toInt()

        private fun key(x: Int, y: Int) = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
    }

    private companion object {
        const val SAMPLE_STEP_M = 2.0
        const val BUCKET_M = 50.0
    }
}
