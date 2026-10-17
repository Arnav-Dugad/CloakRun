package com.arnavgames.cloakrun.core.data

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the App Check debug provider. On first run it logs a debug secret; add it in the
 * Firebase console (see docs/FIREBASE_SETUP.md, App Check). App Check is in monitor mode, so
 * nothing breaks before that.
 */
internal object AppCheckProviders {
    fun factory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
