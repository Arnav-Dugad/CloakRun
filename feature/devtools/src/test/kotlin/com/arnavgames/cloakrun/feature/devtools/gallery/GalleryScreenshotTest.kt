package com.arnavgames.cloakrun.feature.devtools.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of every Design Gallery section in the Night and Daylight themes. Goldens live in
 * src/test/screenshots. Record with `./gradlew :feature:devtools:recordRoborazziDebug` and verify
 * with `verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h1500dp-xxhdpi")
class GalleryScreenshotTest(private val index: Int, private val theme: ThemePreference) {
    @Test
    fun section() {
        val (_, content) = GallerySections[index]
        captureRoboImage(
            filePath = "src/test/screenshots/gallery_${index.toString().padStart(2, '0')}_${theme.name.lowercase()}.png",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
            ),
        ) {
            CloakTheme(preference = theme, reduceMotion = true) {
                GlassScene(backdrop = { GalleryBackdrop() }) {
                    Box(Modifier.fillMaxWidth().padding(20.dp)) { content() }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun params(): List<Array<Any>> = GallerySections.indices.flatMap { i ->
            listOf(ThemePreference.Night, ThemePreference.Daylight).map { arrayOf<Any>(i, it) }
        }
    }
}
