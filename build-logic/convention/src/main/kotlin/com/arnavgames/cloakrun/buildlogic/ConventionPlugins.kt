package com.arnavgames.cloakrun.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        extensions.configure<ApplicationExtension> { configureApplication(this) }
        configureQuality()
    }
}

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> { configureLibrary(this) }
        configureQuality()
        dependencies {
            add("testImplementation", libs.lib("robolectric"))
            add("testImplementation", libs.lib("junit4"))
            add("testImplementation", libs.lib("truth"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            add("testImplementation", libs.lib("turbine"))
        }
    }
}

/**
 * Enables Jetpack Compose on an Android application or library module.
 * Apply after `cloakrun.android.application` or `cloakrun.android.library`.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { buildFeatures.compose = true }
        }
        extensions.configure<ComposeCompilerGradlePluginExtension> {
            stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("config/compose/stability.conf"))
            if (providers.gradleProperty("cloakrun.composeReports").map(String::toBoolean).getOrElse(false)) {
                reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
                metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
            }
        }
        dependencies {
            val bom = libs.lib("androidx-compose-bom")
            add("implementation", platform(bom))
            add("testImplementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
            add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
            add("testImplementation", libs.lib("junit4"))
            add("testImplementation", libs.lib("robolectric"))
            add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
            add("testImplementation", libs.lib("roborazzi"))
            add("testImplementation", libs.lib("roborazzi-compose"))
            add("testImplementation", libs.lib("roborazzi-junit-rule"))
        }
    }
}

/** A feature module: Android library with Compose, Hilt, the design system and lifecycle helpers. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("cloakrun.android.library")
        pluginManager.apply("cloakrun.android.compose")
        pluginManager.apply("cloakrun.hilt")
        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("kotlinx-collections-immutable"))
        }
    }
}

/** A pure Kotlin/JVM module with no Android dependency (for example core:game). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureKotlinJvm()
        configureQuality()
        dependencies {
            add("testImplementation", libs.lib("junit4"))
            add("testImplementation", libs.lib("truth"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            add("testImplementation", libs.lib("turbine"))
        }
        tasks.withType<Test>().configureEach { useJUnit() }
    }
}

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")
        dependencies {
            add("implementation", libs.lib("hilt-android"))
            add("ksp", libs.lib("hilt-compiler"))
        }
    }
}

/**
 * Static analysis applied to every module. Formatting (Spotless + ktlint) is configured once
 * at the root; detekt runs per module.
 */
internal fun Project.configureQuality() {
    pluginManager.apply("dev.detekt")
    configureDetekt()
}
