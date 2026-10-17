package com.arnavgames.cloakrun.core.game.safety

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.area.PlayAreaRules
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.geo.LocalFrame
import com.arnavgames.cloakrun.core.game.geo.Planar
import com.arnavgames.cloakrun.core.game.geo.Vec2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** The outcome of snapping to a park. */
sealed interface SnapResult {
    data class Snapped(val area: PlayArea.Polygon, val cutAtHazards: Boolean) : SnapResult

    /** The tap was not inside any park or green space. */
    data object NotAPark : SnapResult

    /** The tapped green space is smaller than the minimum play area. */
    data object TooSmall : SnapResult
}

/**
 * Snaps a play area to the real boundary of the park the host tapped.
 *
 * Map features arrive as tile-clipped pieces, so instead of merging polygons the pieces are
 * rasterised onto a local metric grid. Major roads, rail and water are carved out first, so a
 * park split by a road snaps only to the side the host tapped. The connected region under the tap
 * is flood-filled, its holes are filled, and its outer boundary is traced and simplified to a
 * valid polygon (no crossing edges, at most [PlayAreaRules.maxVertices] corners).
 */
object ParkSnap {
    private const val MAX_CELLS_PER_SIDE = 420
    private const val MIN_CELL_M = 3.0

    fun snap(
        tap: GeoPoint,
        parkPieces: List<List<GeoPoint>>,
        barriers: List<Hazard>,
        rules: PlayAreaRules = PlayAreaRules(),
    ): SnapResult {
        if (parkPieces.isEmpty()) return SnapResult.NotAPark
        val frame = LocalFrame(tap)
        val rings = parkPieces.map { r -> r.map(frame::toLocal) }
        val grid = gridAround(rings.flatten())
        val park = BooleanArray(grid.size)
        rings.forEach { grid.fillPolygon(it, park) }
        val blocked = BooleanArray(grid.size)
        barriers.filter { it.kind != HazardKind.Building }.forEach { h ->
            when (h) {
                is Hazard.Area -> grid.fillPolygon(h.ring.map(frame::toLocal), blocked)
                is Hazard.Corridor -> grid.fillCorridor(h.line.map(frame::toLocal), h.halfWidthM, blocked)
            }
        }
        val boundary = regionAtTap(grid, park, blocked)?.let(grid::traceOuterBoundary) ?: return SnapResult.NotAPark
        val cutAtHazards = (0 until grid.size).any { park[it] && blocked[it] }
        val simplified = simplifyToLimit(boundary, rules.maxVertices, grid.cell)
        val polygon = simplified.takeIf { it.size >= 3 }?.let { PlayArea.Polygon(it.map(frame::toGeo)) }
        return if (polygon == null || polygon.areaM2 < rules.minAreaM2) {
            SnapResult.TooSmall
        } else {
            SnapResult.Snapped(polygon, cutAtHazards)
        }
    }

    private fun gridAround(points: List<Vec2>): Grid {
        val minX = points.minOf(Vec2::x)
        val minY = points.minOf(Vec2::y)
        val width = points.maxOf(Vec2::x) - minX
        val height = points.maxOf(Vec2::y) - minY
        val cell = max(MIN_CELL_M, max(width, height) / MAX_CELLS_PER_SIDE)
        return Grid(
            cols = ceil(width / cell).toInt() + 3,
            rows = ceil(height / cell).toInt() + 3,
            originX = minX - cell,
            originY = minY - cell,
            cell = cell,
        )
    }

    /** The hole-free park region connected to the tap (the local origin), or null if the tap is not on open park. */
    private fun regionAtTap(
        grid: Grid,
        park: BooleanArray,
        blocked: BooleanArray,
    ): BooleanArray? {
        val passable = BooleanArray(grid.size) { park[it] && !blocked[it] }
        val start = grid.indexOf(Vec2(0.0, 0.0))?.takeIf { passable[it] } ?: return null
        return grid.floodFill(start, passable).also(grid::fillHoles)
    }

    /** Douglas-Peucker with a growing tolerance until the ring fits the corner limit without crossing itself. */
    internal fun simplifyToLimit(
        ring: List<Vec2>,
        maxVertices: Int,
        cell: Double,
    ): List<Vec2> {
        var tolerance = cell * 0.75
        var result = ring
        repeat(MAX_SIMPLIFY_ROUNDS) {
            val candidate = simplifyClosed(ring, tolerance)
            if (candidate.size in 3..maxVertices && !Planar.isSelfIntersecting(candidate)) return candidate
            if (candidate.size >= 3) result = candidate
            tolerance *= 1.35
        }
        return result
    }

    private const val MAX_SIMPLIFY_ROUNDS = 40

    private fun simplifyClosed(ring: List<Vec2>, tolerance: Double): List<Vec2> {
        if (ring.size <= 4) return ring
        // Split the closed ring at its two farthest-apart points and simplify each half.
        val a = 0
        val b = ring.indices.maxBy { (ring[it] - ring[a]).length }
        val first = douglasPeucker(ring.subList(a, b + 1), tolerance)
        val second = douglasPeucker(ring.subList(b, ring.size) + ring[a], tolerance)
        return (first.dropLast(1) + second.dropLast(1))
    }

    private fun douglasPeucker(points: List<Vec2>, tolerance: Double): List<Vec2> {
        if (points.size < 3) return points
        var maxDist = 0.0
        var index = 0
        for (i in 1 until points.lastIndex) {
            val d = Planar.distanceToSegment(points[i], points.first(), points.last())
            if (d > maxDist) {
                maxDist = d
                index = i
            }
        }
        if (maxDist <= tolerance) return listOf(points.first(), points.last())
        val left = douglasPeucker(points.subList(0, index + 1), tolerance)
        val right = douglasPeucker(points.subList(index, points.size), tolerance)
        return left.dropLast(1) + right
    }

    /** A boolean raster over a local metric frame. Cell (c, r) covers [x0 + c*cell, x0 + (c+1)*cell). */
    internal class Grid(
        val cols: Int,
        val rows: Int,
        val originX: Double,
        val originY: Double,
        val cell: Double,
    ) {
        val size: Int get() = cols * rows

        fun indexOf(p: Vec2): Int? {
            val c = floor((p.x - originX) / cell).toInt()
            val r = floor((p.y - originY) / cell).toInt()
            return if (c in 0 until cols && r in 0 until rows) r * cols + c else null
        }

        private fun center(c: Int, r: Int) = Vec2(originX + (c + 0.5) * cell, originY + (r + 0.5) * cell)

        /** Even-odd scanline fill of a polygon at cell centres. */
        fun fillPolygon(ring: List<Vec2>, into: BooleanArray) {
            if (ring.size < 3) return
            for (r in 0 until rows) {
                val y = originY + (r + 0.5) * cell
                val crossings = mutableListOf<Double>()
                for (i in ring.indices) {
                    val a = ring[i]
                    val b = ring[(i + 1) % ring.size]
                    if ((a.y > y) != (b.y > y)) crossings += a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y)
                }
                crossings.sort()
                var k = 0
                while (k + 1 < crossings.size) {
                    val c0 = ceil((crossings[k] - originX) / cell - 0.5).toInt().coerceAtLeast(0)
                    val c1 = floor((crossings[k + 1] - originX) / cell - 0.5).toInt().coerceAtMost(cols - 1)
                    for (c in c0..c1) into[r * cols + c] = true
                    k += 2
                }
            }
        }

        /** Marks cells whose centre lies within [halfWidth] of the polyline. */
        fun fillCorridor(
            line: List<Vec2>,
            halfWidth: Double,
            into: BooleanArray,
        ) {
            for (i in 0 until line.lastIndex) {
                val a = line[i]
                val b = line[i + 1]
                val c0 = floor((minOf(a.x, b.x) - halfWidth - originX) / cell).toInt().coerceAtLeast(0)
                val c1 = floor((maxOf(a.x, b.x) + halfWidth - originX) / cell).toInt().coerceAtMost(cols - 1)
                val r0 = floor((minOf(a.y, b.y) - halfWidth - originY) / cell).toInt().coerceAtLeast(0)
                val r1 = floor((maxOf(a.y, b.y) + halfWidth - originY) / cell).toInt().coerceAtMost(rows - 1)
                for (r in r0..r1) {
                    for (c in c0..c1) {
                        if (Planar.distanceToSegment(center(c, r), a, b) <= halfWidth) into[r * cols + c] = true
                    }
                }
            }
        }

        fun floodFill(start: Int, passable: BooleanArray): BooleanArray {
            val region = BooleanArray(size)
            val stack = ArrayDeque<Int>()
            stack.addLast(start)
            region[start] = true
            while (stack.isNotEmpty()) {
                val i = stack.removeLast()
                val c = i % cols
                val r = i / cols
                for ((dc, dr) in NEIGHBOURS) {
                    val nc = c + dc
                    val nr = r + dr
                    if (nc !in 0 until cols || nr !in 0 until rows) continue
                    val n = nr * cols + nc
                    if (!region[n] && passable[n]) {
                        region[n] = true
                        stack.addLast(n)
                    }
                }
            }
            return region
        }

        /** Marks every cell not reachable from the border without crossing the region as part of it. */
        fun fillHoles(region: BooleanArray) {
            val outside = BooleanArray(size)
            val stack = ArrayDeque<Int>()
            for (c in 0 until cols) {
                stack += c
                stack += (rows - 1) * cols + c
            }
            for (r in 0 until rows) {
                stack += r * cols
                stack += r * cols + cols - 1
            }
            while (stack.isNotEmpty()) {
                val i = stack.removeLast()
                if (outside[i] || region[i]) continue
                outside[i] = true
                val c = i % cols
                val r = i / cols
                for ((dc, dr) in NEIGHBOURS) {
                    val nc = c + dc
                    val nr = r + dr
                    if (nc in 0 until cols && nr in 0 until rows) stack.addLast(nr * cols + nc)
                }
            }
            for (i in 0 until size) if (!outside[i]) region[i] = true
        }

        /**
         * Traces the outer boundary of a hole-free region along cell edges, counter-clockwise
         * with the region on the left. Returns corner points in metres.
         */
        fun traceOuterBoundary(region: BooleanArray): List<Vec2>? {
            closeSaddles(region)
            val next = boundaryEdges(region)
            if (next.isEmpty()) return null
            val startKey = next.keys.minWith(compareBy<Long>({ it.toInt() }, { (it shr 32).toInt() }))
            val loop = mutableListOf<Vec2>()
            var k = startKey
            do {
                val x = (k shr 32).toInt()
                val y = k.toInt()
                loop += Vec2(originX + x * cell, originY + y * cell)
                k = next.remove(k) ?: break
            } while (k != startKey && loop.size <= size * 4)
            return removeCollinear(loop)
        }

        private fun isSet(
            region: BooleanArray,
            c: Int,
            r: Int,
        ) = c in 0 until cols && r in 0 until rows && region[r * cols + c]

        /** Directed boundary edges between lattice corners, keyed by their start corner. */
        private fun boundaryEdges(region: BooleanArray): HashMap<Long, Long> {
            val next = HashMap<Long, Long>()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (!region[r * cols + c]) continue
                    if (!isSet(region, c, r - 1)) next[cornerKey(c, r)] = cornerKey(c + 1, r)
                    if (!isSet(region, c + 1, r)) next[cornerKey(c + 1, r)] = cornerKey(c + 1, r + 1)
                    if (!isSet(region, c, r + 1)) next[cornerKey(c + 1, r + 1)] = cornerKey(c, r + 1)
                    if (!isSet(region, c - 1, r)) next[cornerKey(c, r + 1)] = cornerKey(c, r)
                }
            }
            return next
        }

        /**
         * Cells that touch only at a corner would give that corner two outgoing boundary edges.
         * Filling one of the two gaps joins them, which also matches how people walk between them.
         */
        private fun closeSaddles(region: BooleanArray) {
            var changed = true
            while (changed) {
                changed = false
                for (r in 0 until rows - 1) {
                    for (c in 0 until cols - 1) {
                        val a = region[r * cols + c]
                        val b = region[r * cols + c + 1]
                        val d = region[(r + 1) * cols + c]
                        val e = region[(r + 1) * cols + c + 1]
                        // Diagonal pairs match each other but not the other diagonal.
                        val diagonal = a == e && b == d
                        if (diagonal && a != b) {
                            region[r * cols + c + 1] = true
                            changed = true
                        }
                    }
                }
            }
        }

        private fun removeCollinear(points: List<Vec2>): List<Vec2> = points.filterIndexed { i, p ->
            val prev = points[(i - 1 + points.size) % points.size]
            val next = points[(i + 1) % points.size]
            ((p - prev) cross (next - p)) != 0.0
        }

        private companion object {
            val NEIGHBOURS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        }
    }
}

/** Packs a lattice corner into one map key. */
private fun cornerKey(x: Int, y: Int) = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
