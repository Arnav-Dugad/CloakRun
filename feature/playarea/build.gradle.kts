plugins {
    alias(libs.plugins.cloakrun.android.feature)
}

android {
    namespace = "com.arnavgames.cloakrun.feature.playarea"
}

dependencies {
    implementation(projects.core.map)
    implementation(projects.core.location)
    implementation(projects.core.data)
    implementation(libs.androidx.activity.compose)
}
