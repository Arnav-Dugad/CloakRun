package com.arnavgames.cloakrun.core.game.sim

import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.geo.LocalFrame
import kotlin.math.floor

/**
 * An undirected walking graph built from map line features (streets and paths). Line
 * endpoints and vertices closer than the snap distance are merged so that separate features
 * join at junctions.
 */
class RoadGraph internal constructor(val nodes: List<GeoPoint>, private val adjacency: List<IntArray>) {
    val edgeCount: Int get() = adjacency.sumOf { it.size } / 2

    fun neighbours(node: Int): IntArray = adjacency[node]

    fun nearestNode(p: GeoPoint): Int = nodes.indices.minBy { Geo.distance(nodes[it], p) }

    /** Node indices of the largest connected component, so bots never get trapped on islands. */
    fun largestComponent(): Set<Int> {
        val seen = BooleanArray(nodes.size)
        var best = emptySet<Int>()
        for (start in nodes.indices) {
            if (seen[start]) continue
            val component = mutableSetOf<Int>()
            val queue = ArrayDeque(listOf(start))
            seen[start] = true
            while (queue.isNotEmpty()) {
                val n = queue.removeFirst()
                component += n
                for (m in adjacency[n]) {
                    if (!seen[m]) {
                        seen[m] = true
                        queue += m
                    }
                }
            }
            if (component.size > best.size) best = component
        }
        return best
    }

    companion object {
        fun build(polylines: List<List<GeoPoint>>, snapM: Double = 4.0): RoadGraph {
            val points = polylines.flatten()
            if (points.isEmpty()) return RoadGraph(emptyList(), emptyList())
            val frame = LocalFrame(points.first())
            val nodes = mutableListOf<GeoPoint>()
            val edges = mutableListOf<MutableSet<Int>>()
            val grid = HashMap<Long, MutableList<Int>>()

            fun cellKey(cx: Long, cy: Long) = (cx shl 32) xor (cy and 0xffffffffL)

            fun nodeFor(p: GeoPoint): Int {
                val v = frame.toLocal(p)
                val cx = floor(v.x / snapM).toLong()
                val cy = floor(v.y / snapM).toLong()
                for (dx in -1L..1L) {
                    for (dy in -1L..1L) {
                        grid[cellKey(cx + dx, cy + dy)]?.forEach { id ->
                            if (Geo.distance(nodes[id], p) <= snapM) return id
                        }
                    }
                }
                nodes += p
                edges.add(mutableSetOf())
                val id = nodes.lastIndex
                grid.getOrPut(cellKey(cx, cy)) { mutableListOf() } += id
                return id
            }

            for (line in polylines) {
                var prev = -1
                for (p in line) {
                    val id = nodeFor(p)
                    if (prev >= 0 && prev != id) {
                        edges[prev] += id
                        edges[id] += prev
                    }
                    prev = id
                }
            }
            return RoadGraph(nodes, edges.map { it.toIntArray() })
        }
    }
}
