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
import com.arnavgames.cloakrun.core.game.geo.Simplify
import com.arnavgames.cloakrun.core.game.safety.Hazard
import com.arnavgames.cloakrun.core.game.safety.HazardKind
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.Geometry
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

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

    /**
     * Projects many coordinates in one native call. Returns x, y pairs interleaved, or null before
     * the map is ready. Much cheaper than calling [toScreen] per point for large overlays.
     */
    fun toScreen(points: List<GeoPoint>): FloatArray? {
        val projection = map?.projection ?: return null
        val input = DoubleArray(points.size * 2)
        points.forEachIndexed { i, p ->
            input[i * 2] = p.lat
            input[i * 2 + 1] = p.lng
        }
        val output = DoubleArray(input.size)
        projection.toScreenLocations(input, output)
        return FloatArray(output.size) { output[it].toFloat() }
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

    /**
     * The green space under [tap] (park, grass, wood or leisure land), as all of its rendered
     * pieces. Vector tiles clip features at tile edges, so pieces sharing the tapped feature's id
     * are collected from the whole viewport.
     */
    fun parkPiecesAt(
        tap: Offset,
        widthPx: Float,
        heightPx: Float,
    ): List<List<GeoPoint>> {
        val m = map ?: return emptyList()
        val hit = m.queryRenderedFeatures(PointF(tap.x, tap.y), *PARK_LAYERS).firstOrNull() ?: return emptyList()
        val id = hit.id()
        val pieces = if (id == null) {
            listOf(hit)
        } else {
            m.queryRenderedFeatures(RectF(0f, 0f, widthPx, heightPx), *PARK_LAYERS).filter { it.id() == id }
        }
        return pieces.flatMap { outerRings(it.geometry()) }
    }

    /** Safety hazards rendered in the viewport: water, major roads, rail, buildings and restricted land. */
    fun visibleHazards(widthPx: Float, heightPx: Float): List<Hazard> {
        val m = map ?: return emptyList()
        val rect = RectF(0f, 0f, widthPx, heightPx)
        val hazards = mutableListOf<Hazard>()
        fun areas(kind: HazardKind, vararg layers: String) {
            m.queryRenderedFeatures(rect, *layers).forEach { f ->
                outerRings(f.geometry()).forEach {
                    hazards +=
                        Hazard.Area(kind, Simplify.ring(it, SIMPLIFY_M))
                }
            }
        }
        fun corridors(kind: HazardKind, vararg layers: String) {
            m.queryRenderedFeatures(rect, *layers).forEach { f ->
                val half = Hazard.corridorHalfWidth(kind, f.getStringProperty("class"))
                lines(f.geometry()).forEach { hazards += Hazard.Corridor(kind, Simplify.line(it, SIMPLIFY_M), half) }
            }
        }
        areas(HazardKind.Water, "water")
        areas(HazardKind.Restricted, "landuse-restricted")
        areas(HazardKind.Building, "building-3d", "building-flat")
        corridors(HazardKind.MajorRoad, "road-motorway", "road-primary")
        corridors(HazardKind.Rail, "road-rail")
        return hazards
    }

    /** The current zoom level, or 0 before the map is ready. */
    val zoom: Double get() = map?.cameraPosition?.zoom ?: 0.0

    /**
     * Frames [points] top-down (no tilt or rotation) with [paddingPx] around them, at least at
     * [minZoom] so detail layers such as buildings and footpaths are rendered, and suspends until
     * the camera settles. Used before querying rendered features for the safety pass.
     */
    suspend fun fitAndAwaitIdle(
        points: List<GeoPoint>,
        paddingPx: Int,
        reducedMotion: Boolean,
        minZoom: Double = SAFETY_MIN_ZOOM,
    ) {
        val m = map ?: return
        if (points.size < 2) return
        val bounds = LatLngBounds.Builder().includes(points.map { LatLng(it.lat, it.lng) }).build()
        val padding = intArrayOf(paddingPx, paddingPx, paddingPx, paddingPx)
        val fitted = m.getCameraForLatLngBounds(bounds, padding, 0.0, 0.0) ?: return
        val target = CameraPosition.Builder(fitted).zoom(maxOf(fitted.zoom, minZoom)).tilt(0.0).bearing(0.0).build()
        val update = CameraUpdateFactory.newCameraPosition(target)
        suspendCancellableCoroutine { cont ->
            lateinit var listener: MapLibreMap.OnCameraIdleListener
            listener = MapLibreMap.OnCameraIdleListener {
                m.removeOnCameraIdleListener(listener)
                if (cont.isActive) cont.resume(Unit)
            }
            m.addOnCameraIdleListener(listener)
            if (reducedMotion) m.moveCamera(update) else m.animateCamera(update, FIT_MS)
            cont.invokeOnCancellation { m.removeOnCameraIdleListener(listener) }
        }
    }

    companion object {
        /** Green spaces that can be snapped to. */
        val PARK_LAYERS = arrayOf("park", "landuse-leisure", "landcover-grass", "landcover-wood")
        private const val FIT_MS = 900

        /** Buildings render from zoom 13 and footpaths from 14; query hazards no further out than this. */
        const val SAFETY_MIN_ZOOM = 15.0

        /** Hazard outlines are simplified to this tolerance; well under GPS accuracy. */
        private const val SIMPLIFY_M = 1.5

        private fun toPoints(ring: List<Point>): List<GeoPoint> = ring.map { GeoPoint(it.latitude(), it.longitude()) }

        internal fun outerRings(geometry: Geometry?): List<List<GeoPoint>> = when (geometry) {
            is Polygon -> geometry.coordinates().take(1).map(::toPoints)
            is MultiPolygon -> geometry.coordinates().mapNotNull { it.firstOrNull() }.map(::toPoints)
            else -> emptyList()
        }.filter { it.size >= 3 }

        internal fun lines(geometry: Geometry?): List<List<GeoPoint>> = when (geometry) {
            is LineString -> listOf(toPoints(geometry.coordinates()))
            is MultiLineString -> geometry.coordinates().map(::toPoints)
            else -> emptyList()
        }.filter { it.size >= 2 }

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
