package com.arnavgames.cloakrun.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal val JAVA_VERSION = JavaVersion.VERSION_17
internal val JVM_TARGET = JvmTarget.JVM_17

private val SHARED_OPT_INS = listOf(
    "kotlin.RequiresOptIn",
)

internal fun Project.configureApplication(extension: ApplicationExtension) {
    extension.apply {
        compileSdk = libs.int("compileSdk")
        defaultConfig {
            minSdk = libs.int("minSdk")
            targetSdk = libs.int("targetSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
        lint {
            warningsAsErrors = true
            abortOnError = true
            checkReleaseBuilds = false
            // Version freshness is handled by Dependabot and docs/DECISIONS.md, not by lint.
            disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
        }
        packaging {
            resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
        }
    }
    configureKotlinAndroid()
}

internal fun Project.configureLibrary(extension: LibraryExtension) {
    extension.apply {
        compileSdk = libs.int("compileSdk")
        defaultConfig {
            minSdk = libs.int("minSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
        lint {
            warningsAsErrors = true
            abortOnError = true
            checkReleaseBuilds = false
            // Version freshness is handled by Dependabot and docs/DECISIONS.md, not by lint.
            disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
        }
    }
    configureKotlinAndroid()
}

/**
 * Robolectric (screenshot and Compose UI tests) reaches into java.base internals that JDK 17+
 * no longer exposes by default.
 */
internal fun Project.configureRobolectricJvm() {
    tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
        jvmArgs(
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        )
        maxHeapSize = "2g"
    }
}

private fun Project.configureKotlinAndroid() {
    configureRobolectricJvm()
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JVM_TARGET)
            optIn.addAll(SHARED_OPT_INS)
        }
    }
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget.set(JVM_TARGET)
            optIn.addAll(SHARED_OPT_INS)
        }
    }
}
