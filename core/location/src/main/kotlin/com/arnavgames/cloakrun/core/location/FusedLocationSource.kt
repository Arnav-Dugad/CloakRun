package com.arnavgames.cloakrun.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.location.FixPriority
import com.arnavgames.cloakrun.core.game.location.FixSource
import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.game.location.SamplingPlan
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Device location through Google Play services' fused provider. */
class FusedLocationSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationSource {
    @SuppressLint("MissingPermission") // Checked by hasLocationPermission() before requesting updates.
    override fun fixes(plan: SamplingPlan): Flow<LocationFix> = callbackFlow {
        if (!context.hasLocationPermission()) {
            close(LocationUnavailableException("location permission not granted"))
            return@callbackFlow
        }
        val request = LocationRequest.Builder(plan.priority.toPlayPriority(), plan.intervalMs)
            .setMinUpdateIntervalMillis(plan.minIntervalMs)
            .setMinUpdateDistanceMeters(plan.minDistanceM)
            .setMaxUpdateDelayMillis(plan.maxBatchDelayMs)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toFix()) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { close(LocationUnavailableException(it.message ?: "location request failed")) }
        awaitClose { client.removeLocationUpdates(callback) }
    }
}

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun FixPriority.toPlayPriority(): Int = when (this) {
    FixPriority.HighAccuracy -> Priority.PRIORITY_HIGH_ACCURACY
    FixPriority.Balanced -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
    FixPriority.LowPower -> Priority.PRIORITY_LOW_POWER
}

internal fun Location.toFix(): LocationFix = LocationFix(
    point = GeoPoint(latitude, longitude),
    accuracyM = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
    timeMillis = time,
    speedMps = if (hasSpeed()) speed else null,
    bearingDeg = if (hasBearing()) bearing else null,
    isMock = isMockLocation(),
    source = FixSource.Device,
)

private fun Location.isMockLocation(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    isMock
} else {
    @Suppress("DEPRECATION")
    isFromMockProvider
}
