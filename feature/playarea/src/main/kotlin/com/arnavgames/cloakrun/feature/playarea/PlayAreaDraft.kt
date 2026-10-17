package com.arnavgames.cloakrun.feature.playarea

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.area.PlayAreaCheck
import com.arnavgames.cloakrun.core.game.area.PlayAreaRules
import com.arnavgames.cloakrun.core.game.geo.GeoPoint

/** How the area is drawn. Park snaps to a real green space and then edits like a polygon. */
enum class AreaShape {
    Circle,
    Polygon,
    Park,
    ;

    val usesVertices: Boolean get() = this != Circle
}

/**
 * The area being drawn. Pure state with pure transitions, so the editor's behaviour is unit
 * tested without Android.
 */
data class PlayAreaDraft(
    val shape: AreaShape = AreaShape.Circle,
    val circleCenter: GeoPoint? = null,
    val radiusM: Double = DEFAULT_RADIUS_M,
    val vertices: List<GeoPoint> = emptyList(),
) {
    /** The drawn area, or null while there is not enough to form one. */
    val area: PlayArea?
        get() = when (shape) {
            AreaShape.Circle -> circleCenter?.let { PlayArea.Circle(it, radiusM) }
            AreaShape.Polygon, AreaShape.Park -> if (vertices.size >= 3) PlayArea.Polygon(vertices) else null
        }

    fun check(rules: PlayAreaRules): PlayAreaCheck = area?.let(rules::validate)
        ?: if (shape.usesVertices) PlayAreaCheck.TooFewVertices else PlayAreaCheck.TooSmall

    fun withShape(newShape: AreaShape): PlayAreaDraft = copy(shape = newShape)

    /** A tap on the map: moves the circle centre, or appends a polygon corner (up to the limit). */
    fun tap(point: GeoPoint, rules: PlayAreaRules): PlayAreaDraft = when (shape) {
        AreaShape.Circle -> copy(circleCenter = point)

        AreaShape.Polygon -> if (vertices.size < rules.maxVertices) copy(vertices = vertices + point) else this

        // Park taps are resolved against the map by the screen; see [withSnapped].
        AreaShape.Park -> this
    }

    fun withRadius(meters: Double, rules: PlayAreaRules): PlayAreaDraft =
        copy(radiusM = meters.coerceIn(rules.minCircleRadiusM, rules.maxCircleRadiusM))

    fun moveVertex(index: Int, to: GeoPoint): PlayAreaDraft =
        if (index in vertices.indices) copy(vertices = vertices.toMutableList().also { it[index] = to }) else this

    fun undo(): PlayAreaDraft = if (vertices.isNotEmpty()) copy(vertices = vertices.dropLast(1)) else this

    fun clear(): PlayAreaDraft = when (shape) {
        AreaShape.Circle -> copy(circleCenter = null)
        AreaShape.Polygon, AreaShape.Park -> copy(vertices = emptyList())
    }

    /** Replaces the corners with a snapped park outline. */
    fun withSnapped(outline: List<GeoPoint>): PlayAreaDraft = copy(shape = AreaShape.Park, vertices = outline)

    companion object {
        const val DEFAULT_RADIUS_M = 400.0

        fun from(area: PlayArea): PlayAreaDraft = when (area) {
            is PlayArea.Circle -> PlayAreaDraft(AreaShape.Circle, area.center, area.radiusM)
            is PlayArea.Polygon -> PlayAreaDraft(AreaShape.Polygon, vertices = area.vertices)
        }
    }
}
