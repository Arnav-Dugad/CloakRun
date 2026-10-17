// Pure Kotlin game engine: rules, geometry, simulation. No Android dependency, so every rule is
// deterministic and unit-testable on the JVM.
plugins {
    alias(libs.plugins.cloakrun.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
