package com.arnavgames.cloakrun.core.location

import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.game.location.SamplingInput
import com.arnavgames.cloakrun.core.game.location.SamplingPolicy
import com.arnavgames.cloakrun.core.game.location.TrackingMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn

/**
 * The single place location is read. Callers declare what they are doing ([TrackingMode]) and
 * the repository applies the adaptive [SamplingPolicy] to the active source. Fixes are shared,
 * so the map, the match engine and the foreground service never open duplicate requests.
 *
 * Exact fixes stay on the device: nothing here talks to the network.
 */
@Singleton
class LocationRepository @Inject constructor(deviceSource: FusedLocationSource, conditions: DeviceConditions) {
    private val scope = CoroutineScope(SupervisorJob())
    private val requests = MutableStateFlow<Map<String, TrackingMode>>(emptyMap())
    private val opponentDistance = MutableStateFlow<Double?>(null)
    private val _lastFix = MutableStateFlow<LocationFix?>(null)
    private val _error = MutableStateFlow<Throwable?>(null)

    /** Debug builds may replace the device source (Simulation Mode). Release builds never set it. */
    val sourceOverride = MutableStateFlow<LocationSource?>(null)

    val lastFix: StateFlow<LocationFix?> = _lastFix.asStateFlow()

    /** The most recent reason location could not be read, cleared on the next fix. */
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    /** The most demanding mode any active caller has asked for. */
    val mode: Flow<TrackingMode> = requests.map { r -> r.values.maxByOrNull { it.ordinal } ?: TrackingMode.Off }
        .distinctUntilChanged()

    val fixes: Flow<LocationFix> = combine(mode, conditions.power, opponentDistance, sourceOverride) { mode, power, opponent, override ->
        val plan = SamplingPolicy.plan(
            SamplingInput(
                mode = mode,
                nearestOpponentM = opponent,
                batteryPercent = power.batteryPercent,
                charging = power.charging,
                powerSaveMode = power.powerSaveMode,
            ),
        )
        plan to (override ?: deviceSource)
    }.distinctUntilChanged()
        .flatMapLatest { (plan, source) ->
            if (plan == null) {
                emptyFlow()
            } else {
                source.fixes(plan).catch { _error.value = it }
            }
        }
        .onEach {
            _lastFix.value = it
            _error.value = null
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    /** Registers a caller's need for location. Pair every call with [release] for the same [key]. */
    fun request(key: String, mode: TrackingMode) {
        requests.value = requests.value + (key to mode)
    }

    fun release(key: String) {
        requests.value = requests.value - key
    }

    /** Feeds the policy the current distance band to the nearest opponent (from fuzzed zones). */
    fun updateOpponentDistance(meters: Double?) {
        opponentDistance.value = meters
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 3_000L
    }
}
