package com.arnavgames.cloakrun.feature.home

import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class HomeScreenshotTest(private val theme: ThemePreference) {
    @Test
    fun home() {
        captureRoboImage(
            filePath = "src/test/screenshots/home_${theme.name.lowercase()}.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f)),
        ) {
            CloakTheme(preference = theme, reduceMotion = true) {
                HomeScreen(onDesignArea = {}, onAbout = {})
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params(): List<Array<Any>> = listOf(arrayOf<Any>(ThemePreference.Night), arrayOf<Any>(ThemePreference.Daylight))
    }
}
