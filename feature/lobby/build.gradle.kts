plugins {
    alias(libs.plugins.cloakrun.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.arnavgames.cloakrun.feature.lobby"
}

dependencies {
    implementation(projects.core.data)
    implementation(libs.androidx.activity.compose)
    implementation(libs.zxing.core)
    implementation(libs.play.services.code.scanner)
    implementation(libs.kotlinx.coroutines.play.services)
}
