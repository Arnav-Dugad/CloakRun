package com.arnavgames.cloakrun.core.game.geo

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Planar polygon helpers on [Vec2] rings (metres). Rings are implicitly closed. */
object Planar {
    /** Unsigned area by the shoelace formula, square metres. */
    fun area(ring: List<Vec2>): Double = abs(signedArea(ring))

    fun signedArea(ring: List<Vec2>): Double {
        var sum = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            sum += a cross b
        }
        return sum / 2
    }

    /** Even-odd ray casting. Points exactly on an edge count as inside. */
    fun contains(ring: List<Vec2>, p: Vec2): Boolean {
        if (ring.size < 3) return false
        if (distanceToBoundary(ring, p) < 1e-9) return true
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[j]
            if ((a.y > p.y) != (b.y > p.y)) {
                val xCross = (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
                if (p.x < xCross) inside = !inside
            }
            j = i
        }
        return inside
    }

    /** Shortest distance from [p] to the ring's edges, metres. */
    fun distanceToBoundary(ring: List<Vec2>, p: Vec2): Double {
        var best = Double.MAX_VALUE
        for (i in ring.indices) {
            best = min(best, distanceToSegment(p, ring[i], ring[(i + 1) % ring.size]))
        }
        return best
    }

    fun distanceToSegment(
        p: Vec2,
        a: Vec2,
        b: Vec2,
    ): Double {
        val ab = b - a
        val len2 = ab dot ab
        if (len2 == 0.0) return (p - a).length
        val t = (((p - a) dot ab) / len2).coerceIn(0.0, 1.0)
        return (p - (a + ab * t)).length
    }

    /** True if any two non-adjacent edges cross (a "bow tie"), which makes an area ambiguous. */
    fun isSelfIntersecting(ring: List<Vec2>): Boolean {
        val n = ring.size
        if (n < 4) return false
        for (i in 0 until n) {
            val a1 = ring[i]
            val a2 = ring[(i + 1) % n]
            for (j in i + 1 until n) {
                // Skip edges that share a vertex.
                if (j == i || (j + 1) % n == i || (i + 1) % n == j) continue
                if (segmentsIntersect(a1, a2, ring[j], ring[(j + 1) % n])) return true
            }
        }
        return false
    }

    private fun segmentsIntersect(
        p1: Vec2,
        p2: Vec2,
        q1: Vec2,
        q2: Vec2,
    ): Boolean {
        val d1 = orientation(q1, q2, p1)
        val d2 = orientation(q1, q2, p2)
        val d3 = orientation(p1, p2, q1)
        val d4 = orientation(p1, p2, q2)
        if (straddles(d1, d2) && straddles(d3, d4)) return true
        return touches(d1, q1, q2, p1) || touches(d2, q1, q2, p2) || touches(d3, p1, p2, q1) || touches(d4, p1, p2, q2)
    }

    /** True when the two orientations are on strictly opposite sides. */
    private fun straddles(a: Double, b: Double): Boolean = (a > 0 && b < 0) || (a < 0 && b > 0)

    /** A collinear point that lies on the segment counts as touching. */
    private fun touches(
        orientation: Double,
        a: Vec2,
        b: Vec2,
        p: Vec2,
    ): Boolean = orientation == 0.0 && onSegment(a, b, p)

    private fun orientation(
        a: Vec2,
        b: Vec2,
        c: Vec2,
    ): Double = (b - a) cross (c - a)

    private fun onSegment(
        a: Vec2,
        b: Vec2,
        p: Vec2,
    ): Boolean = p.x in min(a.x, b.x)..max(a.x, b.x) && p.y in min(a.y, b.y)..max(a.y, b.y)
}
