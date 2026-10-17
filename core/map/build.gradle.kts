plugins {
    alias(libs.plugins.cloakrun.android.library)
    alias(libs.plugins.cloakrun.android.compose)
}

android {
    namespace = "com.arnavgames.cloakrun.core.map"
}

dependencies {
    api(projects.core.game)
    api(projects.core.designsystem)
    api(libs.maplibre.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
