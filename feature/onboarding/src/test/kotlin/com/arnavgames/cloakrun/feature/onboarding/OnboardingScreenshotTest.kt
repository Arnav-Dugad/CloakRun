package com.arnavgames.cloakrun.feature.onboarding

import androidx.compose.foundation.text.input.TextFieldState
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
class OnboardingScreenshotTest(private val step: OnboardingStep) {
    @Test
    fun step() {
        captureRoboImage(
            filePath = "src/test/screenshots/onboarding_${step.name.lowercase()}.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f)),
        ) {
            CloakTheme(preference = ThemePreference.Night, reduceMotion = true) {
                OnboardingScreen(
                    state = OnboardingUiState(step = step, teen = true, avatarSeed = "screenshot"),
                    name = TextFieldState("Silent Lynx 37"),
                    actions = OnboardingActions(),
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params(): List<Array<Any>> = listOf(
            OnboardingStep.Age,
            OnboardingStep.Rules,
            OnboardingStep.Consent,
            OnboardingStep.Profile,
            OnboardingStep.Blocked,
        ).map { arrayOf<Any>(it) }
    }
}
