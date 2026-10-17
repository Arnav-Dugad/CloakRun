plugins {
    alias(libs.plugins.cloakrun.android.library)
    alias(libs.plugins.cloakrun.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.arnavgames.cloakrun.feature.home"
}

dependencies {
    implementation(projects.core.designsystem)
}
