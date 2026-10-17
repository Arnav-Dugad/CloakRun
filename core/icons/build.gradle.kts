plugins {
    alias(libs.plugins.cloakrun.android.library)
    alias(libs.plugins.cloakrun.android.compose)
}

android {
    namespace = "com.arnavgames.cloakrun.core.icons"
}

dependencies {
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
}
