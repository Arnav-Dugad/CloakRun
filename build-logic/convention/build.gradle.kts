plugins {
    `kotlin-dsl`
}

group = "com.arnavgames.cloakrun.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "cloakrun.android.application"
            implementationClass = "com.arnavgames.cloakrun.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "cloakrun.android.library"
            implementationClass = "com.arnavgames.cloakrun.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "cloakrun.android.compose"
            implementationClass = "com.arnavgames.cloakrun.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "cloakrun.android.feature"
            implementationClass = "com.arnavgames.cloakrun.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "cloakrun.jvm.library"
            implementationClass = "com.arnavgames.cloakrun.buildlogic.JvmLibraryConventionPlugin"
        }
        register("hilt") {
            id = "cloakrun.hilt"
            implementationClass = "com.arnavgames.cloakrun.buildlogic.HiltConventionPlugin"
        }
    }
}
