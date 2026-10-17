package com.arnavgames.cloakrun.core.map

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arnavgames.cloakrun.core.designsystem.glass.LocalGlassQuality
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * The CloakRun map: MapLibre Native with the custom CloakRun styles, plus a Compose overlay layer
 * ([overlays]) drawn in screen space above it.
 *
 * - Lifecycle is forwarded to the MapView automatically.
 * - With full glass quality the map renders into a TextureView so glass panels can blur it.
 * - Changing [style] crossfades from a snapshot of the old map to the new style.
 */
@Composable
fun CloakMap(
    state: CloakMapState,
    style: MapStyle,
    modifier: Modifier = Modifier,
    onMapClick: ((GeoPoint) -> Unit)? = null,
    overlays: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val textureMode = LocalGlassQuality.current.blurs
    val bottomInsetPx = with(LocalDensity.current) { CloakTheme.spacing.s.roundToPx() }
    val clickHandler by rememberUpdatedState(onMapClick)
    val motion = CloakTheme.motion

    val mapView = remember(textureMode) { createMapView(context, textureMode, bottomInsetPx) }
    var appliedStyle by remember { mutableStateOf<MapStyle?>(null) }
    var fadeFrom by remember { mutableStateOf<Bitmap?>(null) }
    val fade = remember { Animatable(0f) }

    DisposableEffect(mapView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.addOnDidFinishRenderingFrameListener { _, _, _ -> state.frame++ }
        mapView.getMapAsync { map ->
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isRotateGesturesEnabled = true
            map.addOnMapClickListener { latLng ->
                clickHandler?.invoke(GeoPoint(latLng.latitude, latLng.longitude))
                clickHandler != null
            }
            state.map = map
        }
        onDispose {
            lifecycle.removeObserver(observer)
            state.map = null
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(state.map, style) {
        val map = state.map ?: return@LaunchedEffect
        if (appliedStyle == style) return@LaunchedEffect
        val crossfade = appliedStyle != null && !motion.reduced
        appliedStyle = style
        if (crossfade) {
            // Capture the old style first, then switch inside the callback so the snapshot is never the new style.
            map.snapshot { bitmap ->
                fadeFrom = bitmap
                map.setStyle(Style.Builder().fromUri(style.uri))
            }
        } else {
            map.setStyle(Style.Builder().fromUri(style.uri))
        }
    }

    LaunchedEffect(fadeFrom) {
        if (fadeFrom == null) return@LaunchedEffect
        fade.snapTo(1f)
        fade.animateTo(0f, tween(STYLE_CROSSFADE_MS))
        fadeFrom = null
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        fadeFrom?.let { snapshot ->
            Image(
                bitmap = snapshot.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade.value },
            )
        }
        overlays()
    }
}

private fun createMapView(
    context: Context,
    textureMode: Boolean,
    bottomInsetPx: Int,
): MapView {
    MapLibre.getInstance(context.applicationContext)
    val options = MapLibreMapOptions.createFromAttributes(context)
        .textureMode(textureMode)
        .compassEnabled(false)
        .logoEnabled(false)
        .attributionEnabled(true)
        .attributionGravity(Gravity.BOTTOM or Gravity.START)
        .attributionMargins(intArrayOf(bottomInsetPx * 2, 0, 0, bottomInsetPx * 2))
        .foregroundLoadColor("#07061A".toColorInt())
    return MapView(context, options).apply { onCreate(null) }
}

private const val STYLE_CROSSFADE_MS = 700
