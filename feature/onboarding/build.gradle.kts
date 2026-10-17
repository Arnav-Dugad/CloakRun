plugins {
    alias(libs.plugins.cloakrun.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.arnavgames.cloakrun.feature.onboarding"
}

dependencies {
    implementation(projects.core.data)
    implementation(libs.androidx.activity.compose)
}
