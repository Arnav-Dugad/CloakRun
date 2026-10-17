package com.arnavgames.cloakrun.core.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.perf.FirebasePerformance
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single entry point to Firebase. When a build has no google-services.json, Firebase is never
 * initialised and [available] is false: online features explain that instead of crashing.
 */
@Singleton
class Backend @Inject constructor(@param:ApplicationContext private val context: Context) {
    val available: Boolean by lazy { FirebaseApp.getApps(context).isNotEmpty() }

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // google-services.json does not always carry the database URL, so it is pinned here.
    val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance(BackendConfig.DATABASE_URL) }

    /** Called once from Application.onCreate, before anything else touches Firebase. */
    fun initialize(diagnosticsAllowed: Boolean) {
        if (!available) return
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(AppCheckProviders.factory())
        applyDiagnostics(diagnosticsAllowed)
    }

    /**
     * Crash reports, usage statistics and performance traces follow the player's choice on the
     * consent screen (and in Profile). Debug builds never report.
     */
    fun applyDiagnostics(allowed: Boolean) {
        if (!available) return
        val on = allowed && !BuildConfig.DEBUG
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = on
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(on)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = on
    }
}

/** Project values that are identifiers, not secrets. */
object BackendConfig {
    const val DATABASE_URL = "https://cloakrun-a0d71-default-rtdb.asia-southeast1.firebasedatabase.app"

    /** OAuth web client of the Firebase project, used as the server client id for Google sign-in. */
    const val WEB_CLIENT_ID = "848325382398-gs7jb4fj7k9qk6brm7votoadmjsuv8db.apps.googleusercontent.com"
}
