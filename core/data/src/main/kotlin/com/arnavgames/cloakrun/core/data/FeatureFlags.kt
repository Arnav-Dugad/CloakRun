package com.arnavgames.cloakrun.core.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Remote switches (Remote Config). In-app defaults mean nothing needs publishing in the console.
 * Fetched at most every 12 hours, inside the Spark allowance (see QUOTA_BUDGET.md).
 */
@Singleton
class FeatureFlags @Inject constructor(private val backend: Backend) {
    private val config: FirebaseRemoteConfig? by lazy {
        if (!backend.available) {
            null
        } else {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_S).build(),
                )
                setDefaultsAsync(DEFAULTS)
            }
        }
    }

    /** Kill switch for creating and joining lobbies. */
    val matchesEnabled: Boolean get() = config?.getBoolean(MATCHES_ENABLED) ?: true

    /** Kill switch for Realtime Database traffic (presence and zones), for the connection cap. */
    val rtdbEnabled: Boolean get() = config?.getBoolean(RTDB_ENABLED) ?: true

    val rtdbMinWriteIntervalMs: Long get() = config?.getLong(RTDB_MIN_WRITE_MS) ?: DEFAULT_RTDB_MIN_WRITE_MS

    suspend fun refresh() {
        runCatching { config?.fetchAndActivate()?.await() }
    }

    private companion object {
        const val FETCH_INTERVAL_S = 12L * 60 * 60
        const val MATCHES_ENABLED = "matches_enabled"
        const val RTDB_ENABLED = "rtdb_enabled"
        const val RTDB_MIN_WRITE_MS = "rtdb_min_write_interval_ms"
        const val DEFAULT_RTDB_MIN_WRITE_MS = 4_000L
        val DEFAULTS = mapOf<String, Any>(
            MATCHES_ENABLED to true,
            RTDB_ENABLED to true,
            RTDB_MIN_WRITE_MS to DEFAULT_RTDB_MIN_WRITE_MS,
            "appcheck_enforce" to false,
        )
    }
}
