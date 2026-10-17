// Debug-only developer tools: the Design Gallery now, Simulation Mode and the debug overlay
// later. The app depends on this module with debugImplementation only, so none of it ships in
// release builds.
plugins {
    alias(libs.plugins.cloakrun.android.library)
    alias(libs.plugins.cloakrun.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.arnavgames.cloakrun.feature.devtools"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.map)
    implementation(projects.core.location)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
