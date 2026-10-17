package com.arnavgames.cloakrun.core.game.geo

/** Douglas-Peucker simplification of geographic lines and rings, with a tolerance in metres. */
object Simplify {
    fun line(points: List<GeoPoint>, toleranceM: Double): List<GeoPoint> {
        if (points.size < 3) return points
        val frame = LocalFrame(points.first())
        val local = points.map(frame::toLocal)
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        mark(local, 0, points.lastIndex, toleranceM, keep)
        return points.filterIndexed { i, _ -> keep[i] }
    }

    /** Simplifies a closed ring, never returning fewer than three points. */
    fun ring(points: List<GeoPoint>, toleranceM: Double): List<GeoPoint> {
        if (points.size <= 4) return points
        val simplified = line(points + points.first(), toleranceM).dropLast(1)
        return if (simplified.size >= 3) simplified else points
    }

    private fun mark(
        points: List<Vec2>,
        from: Int,
        to: Int,
        tolerance: Double,
        keep: BooleanArray,
    ) {
        var stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(from to to)
        while (stack.isNotEmpty()) {
            val (a, b) = stack.removeLast()
            var maxDist = 0.0
            var index = -1
            for (i in a + 1 until b) {
                val d = Planar.distanceToSegment(points[i], points[a], points[b])
                if (d > maxDist) {
                    maxDist = d
                    index = i
                }
            }
            if (index >= 0 && maxDist > tolerance) {
                keep[index] = true
                stack.addLast(a to index)
                stack.addLast(index to b)
            }
        }
    }
}
