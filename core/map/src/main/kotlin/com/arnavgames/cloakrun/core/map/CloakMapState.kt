package com.arnavgames.cloakrun.core.map

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString

/**
 * Holds the live map and exposes what Compose overlays need: projection between coordinates and
 * screen pixels, a frame tick that changes every rendered map frame (read it in draw scope to
 * redraw overlays in sync with the map), and camera moves.
 */
@Stable
class CloakMapState internal constructor() {
    internal var map: MapLibreMap? by mutableStateOf(null)

    /** Increments after every rendered map frame. Reading it in a draw block keeps overlays glued to the map. */
    var frame: Long by mutableLongStateOf(0L)
        internal set

    /** True once the first style has loaded and the map can be projected. */
    val isReady: Boolean get() = map != null

    /** Coordinates to screen pixels, or null before the map is ready. */
    fun toScreen(point: GeoPoint): Offset? {
        val p = map?.projection?.toScreenLocation(LatLng(point.lat, point.lng)) ?: return null
        return Offset(p.x, p.y)
    }

    /** Screen pixels to coordinates, or null before the map is ready. */
    fun toGeo(offset: Offset): GeoPoint? {
        val ll = map?.projection?.fromScreenLocation(PointF(offset.x, offset.y)) ?: return null
        return GeoPoint(ll.latitude.coerceIn(-90.0, 90.0), ll.longitude.coerceIn(-180.0, 180.0))
    }

    /** Metres per screen pixel at [latitude] for the current zoom. */
    fun metersPerPixel(latitude: Double): Double = map?.projection?.getMetersPerPixelAtLatitude(latitude) ?: 1.0

    /** The current camera target, or null before the map is ready. */
    val center: GeoPoint?
        get() = map?.cameraPosition?.target?.let { GeoPoint(it.latitude, it.longitude) }

    val bearing: Double get() = map?.cameraPosition?.bearing ?: 0.0

    fun moveTo(point: GeoPoint, zoom: Double? = null) {
        val m = map ?: return
        val builder = CameraPosition.Builder(m.cameraPosition).target(LatLng(point.lat, point.lng))
        if (zoom != null) builder.zoom(zoom)
        m.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()))
    }

    fun animateTo(
        point: GeoPoint,
        zoom: Double? = null,
        tilt: Double? = null,
        durationMs: Int = 900,
    ) {
        val m = map ?: return
        val builder = CameraPosition.Builder(m.cameraPosition).target(LatLng(point.lat, point.lng))
        if (zoom != null) builder.zoom(zoom)
        if (tilt != null) builder.tilt(tilt)
        m.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()), durationMs)
    }

    /**
     * The match-start fly-in: starts high and rotated, then swoops down and tilts so the 3D
     * buildings rise into view. With [reducedMotion] it jumps straight to the final framing.
     */
    fun cinematicFlyIn(
        target: GeoPoint,
        reducedMotion: Boolean,
        finalZoom: Double = 16.4,
    ) {
        val m = map ?: return
        val end = CameraPosition.Builder()
            .target(LatLng(target.lat, target.lng))
            .zoom(finalZoom)
            .tilt(FLY_IN_TILT)
            .bearing(FLY_IN_END_BEARING)
            .build()
        if (reducedMotion) {
            m.moveCamera(CameraUpdateFactory.newCameraPosition(end))
            return
        }
        val start = CameraPosition.Builder()
            .target(LatLng(target.lat, target.lng))
            .zoom(finalZoom - FLY_IN_ZOOM_SPAN)
            .tilt(0.0)
            .bearing(FLY_IN_START_BEARING)
            .build()
        m.moveCamera(CameraUpdateFactory.newCameraPosition(start))
        m.animateCamera(CameraUpdateFactory.newCameraPosition(end), FLY_IN_MS)
    }

    /**
     * Walkable street and path geometry currently rendered in the viewport, as polylines. Used by
     * Simulation Mode bots and, later, to place cache points only on walkable features.
     */
    fun visibleWalkableLines(widthPx: Float, heightPx: Float): List<List<GeoPoint>> {
        val m = map ?: return emptyList()
        val features = m.queryRenderedFeatures(RectF(0f, 0f, widthPx, heightPx), *WALKABLE_LAYERS)
        val lines = mutableListOf<List<GeoPoint>>()
        for (feature in features) {
            when (val g = feature.geometry()) {
                is LineString -> lines += g.coordinates().map { GeoPoint(it.latitude(), it.longitude()) }
                is MultiLineString -> g.coordinates().forEach { part -> lines += part.map { GeoPoint(it.latitude(), it.longitude()) } }
                else -> Unit
            }
        }
        return lines.filter { it.size >= 2 }
    }

    companion object {
        /** Style layers that are safe and plausible to walk along. Primary roads and motorways are excluded. */
        val WALKABLE_LAYERS = arrayOf("road-path", "road-minor", "road-service", "road-secondary")
        private const val FLY_IN_MS = 2_600
        private const val FLY_IN_TILT = 55.0
        private const val FLY_IN_ZOOM_SPAN = 4.0
        private const val FLY_IN_START_BEARING = -40.0
        private const val FLY_IN_END_BEARING = 15.0
    }
}

@Composable
fun rememberCloakMapState(): CloakMapState = remember { CloakMapState() }
