package com.arnavgames.cloakrun.buildlogic

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureDetekt() {
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        parallel.set(true)
        config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
        basePath.set(rootProject.layout.projectDirectory)
    }
    dependencies {
        add("detektPlugins", libs.lib("compose-rules-detekt"))
    }
}
