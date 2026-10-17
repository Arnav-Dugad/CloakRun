// Root build. Plugins are declared here with `apply false` so a single version of each is on the
// build classpath; modules apply them through the convention plugins in build-logic.
//
// AGP 9 ships built-in Kotlin support: `org.jetbrains.kotlin.android` must NOT be applied anywhere.
// Declaring kotlin-jvm here pins the Kotlin Gradle Plugin that AGP's built-in Kotlin uses.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.spotless)
}

// ktlint settings are passed explicitly (mirroring .editorconfig, which the IDE reads) so formatting
// is identical on every OS and in CI.
val ktlintRules = mapOf(
    "ktlint_code_style" to "android_studio",
    "max_line_length" to "140",
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "3",
    "ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "3",
    "compose_allowed_composition_locals" to listOf(
        "LocalCloakColors",
        "LocalCloakTypography",
        "LocalCloakSpacing",
        "LocalCloakRadii",
        "LocalCloakSizes",
        "LocalCloakBlur",
        "LocalCloakMotion",
        "LocalGlassBackdrop",
        "LocalGlassQuality",
        "LocalTilt",
        "LocalCloakHaptics",
    ).joinToString(","),
)

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules = libs.compose.rules.ktlint.get().toString()
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
        ktlint(ktlintVersion)
            .editorConfigOverride(ktlintRules)
            .customRuleSets(listOf(composeRules))
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintRules - "compose_allowed_composition_locals")
    }
    format("misc") {
        target("**/*.md", "**/.gitignore", "**/*.yml", "**/*.toml")
        targetExclude("**/build/**", "**/node_modules/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
