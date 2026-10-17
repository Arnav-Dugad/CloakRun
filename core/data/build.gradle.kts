// Firebase-backed data: accounts, profiles, lobbies and presence. Everything Firebase lives behind
// this module, so features never touch the SDKs directly and the app still runs offline when a
// build has no google-services.json.
plugins {
    alias(libs.plugins.cloakrun.android.library)
    alias(libs.plugins.cloakrun.hilt)
}

android {
    namespace = "com.arnavgames.cloakrun.core.data"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.core.game)
    api(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.core.ktx)
}
