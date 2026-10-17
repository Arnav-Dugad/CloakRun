package com.arnavgames.cloakrun.core.game.sim

import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import kotlin.random.Random

/**
 * A simulated player that walks the [RoadGraph] like a person: it follows streets and paths at
 * walking pace with small speed variations, rarely doubles back, pauses now and then, and
 * prefers to stay where [allowed] says (for example inside the play area).
 */
class BotWalker(
    private val graph: RoadGraph,
    private val random: Random,
    startNode: Int,
    private val baseSpeedMps: Double = 1.4,
    private val allowed: (GeoPoint) -> Boolean = { true },
) {
    private var from = startNode
    private var to = pickNext(startNode, previous = -1)
    private var travelled = 0.0
    private var pauseLeftS = 0.0
    private var speed = baseSpeedMps

    var position: GeoPoint = graph.nodes[startNode]
        private set

    /** Heading in degrees clockwise from north, along the current street. */
    var headingDeg: Double = Geo.bearing(graph.nodes[from], graph.nodes[to])
        private set

    val isPaused: Boolean get() = pauseLeftS > 0

    /** Advances the walk by [dtS] seconds of (possibly fast-forwarded) game time. */
    fun advance(dtS: Double) {
        var remaining = dtS
        while (remaining > 0) {
            if (pauseLeftS > 0) {
                val used = minOf(pauseLeftS, remaining)
                pauseLeftS -= used
                remaining -= used
                continue
            }
            val a = graph.nodes[from]
            val b = graph.nodes[to]
            val edge = Geo.distance(a, b)
            val step = speed * remaining
            if (travelled + step < edge || (edge == 0.0 && from == to)) {
                travelled += step
                remaining = 0.0
                position = if (edge > 0) Geo.lerp(a, b, travelled / edge) else a
            } else {
                remaining -= (edge - travelled) / speed
                arrive()
            }
        }
    }

    private fun arrive() {
        val previous = from
        from = to
        to = pickNext(from, previous)
        travelled = 0.0
        position = graph.nodes[from]
        headingDeg = Geo.bearing(graph.nodes[from], graph.nodes[to])
        speed = baseSpeedMps * (0.85 + random.nextDouble() * 0.3)
        if (random.nextDouble() < PAUSE_CHANCE) pauseLeftS = 2.0 + random.nextDouble() * 6.0
    }

    private fun pickNext(node: Int, previous: Int): Int {
        val options = graph.neighbours(node)
        if (options.isEmpty()) return node
        val inside = options.filter { allowed(graph.nodes[it]) }
        val pool = inside.ifEmpty { options.toList() }
        val forward = pool.filter { it != previous }
        return (forward.ifEmpty { pool }).random(random)
    }

    private companion object {
        const val PAUSE_CHANCE = 0.08
    }
}
