package com.arnavgames.cloakrun

import android.app.Application
import com.arnavgames.cloakrun.core.data.Backend
import com.arnavgames.cloakrun.core.data.FeatureFlags
import com.arnavgames.cloakrun.core.data.PlayerPrefs
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class CloakRunApplication : Application() {
    @Inject lateinit var backend: Backend

    @Inject lateinit var prefs: PlayerPrefs

    @Inject lateinit var flags: FeatureFlags

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Diagnostics start off; the player's saved choice is applied as soon as it is read.
        backend.initialize(diagnosticsAllowed = false)
        scope.launch {
            backend.applyDiagnostics(prefs.current().diagnostics)
            flags.refresh()
        }
    }
}
