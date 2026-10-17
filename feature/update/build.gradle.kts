// In-app updater for the sideload flavour (GitHub Releases). The play flavour never depends on
// this module: Google Play restricts REQUEST_INSTALL_PACKAGES and updates itself.
plugins {
    alias(libs.plugins.cloakrun.android.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.arnavgames.cloakrun.feature.update"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
