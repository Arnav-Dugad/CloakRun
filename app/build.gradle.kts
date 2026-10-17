import java.util.Properties

plugins {
    alias(libs.plugins.cloakrun.android.application)
    alias(libs.plugins.cloakrun.android.compose)
    alias(libs.plugins.cloakrun.hilt)
    alias(libs.plugins.kotlin.serialization)
}

val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use(::load)
}

/**
 * Release signing comes from environment variables in CI (decoded from GitHub secrets) or from a
 * git-ignored keystore.properties at the repo root for local release builds. The keystore itself
 * always lives outside the repository. Without either, release builds are produced unsigned.
 */
val signingProps = Properties().apply {
    val local = rootProject.file("keystore.properties")
    if (local.exists()) local.inputStream().use(::load)
}

fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: signingProps.getProperty(prop)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("CLOAKRUN_KEYSTORE_PATH", "storeFile")
val hasReleaseSigning = releaseStoreFile != null && file(releaseStoreFile).exists()

android {
    namespace = "com.arnavgames.cloakrun"

    defaultConfig {
        applicationId = "com.arnavgames.cloakrun"
        versionCode = versionProps.getProperty("VERSION_CODE").toInt()
        versionName = versionProps.getProperty("VERSION_NAME")
    }

    androidResources {
        localeFilters += listOf("en")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = signingValue("CLOAKRUN_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("CLOAKRUN_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("CLOAKRUN_KEY_PASSWORD", "keyPassword")
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        // GitHub Releases APK. Ships the in-app updater (REQUEST_INSTALL_PACKAGES).
        create("sideload") {
            dimension = "distribution"
            isDefault = true
            buildConfigField("boolean", "UPDATER_ENABLED", "true")
        }
        // Google Play. No updater: Play restricts the install-packages permission.
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER_ENABLED", "false")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

// Firebase is optional for local builds: google-services.json is git-ignored and injected in CI
// from a secret. Without it the app builds and runs fully offline (see docs/FIREBASE_SETUP.md).
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.location)
    implementation(projects.core.map)
    implementation(projects.feature.home)
    implementation(projects.feature.playarea)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.lobby)
    implementation(projects.feature.profile)
    debugImplementation(projects.feature.devtools)
    "sideloadImplementation"(projects.feature.update)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}
