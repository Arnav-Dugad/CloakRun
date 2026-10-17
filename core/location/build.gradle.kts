plugins {
    alias(libs.plugins.cloakrun.android.library)
    alias(libs.plugins.cloakrun.hilt)
}

android {
    namespace = "com.arnavgames.cloakrun.core.location"
}

dependencies {
    api(projects.core.game)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
}
