plugins {
    alias(libs.plugins.cloakrun.android.library)
    alias(libs.plugins.cloakrun.android.compose)
}

android {
    namespace = "com.arnavgames.cloakrun.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
}
