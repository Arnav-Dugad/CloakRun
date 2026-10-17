package com.arnavgames.cloakrun.feature.profile

import androidx.compose.foundation.text.input.TextFieldState
import com.arnavgames.cloakrun.core.data.AuthUser
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ProfileScreenshotTest {
    private fun shot(name: String, state: ProfileUiState) = captureRoboImage(
        filePath = "src/test/screenshots/$name.png",
        roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f)),
    ) {
        CloakTheme(preference = ThemePreference.Night, reduceMotion = true) {
            ProfileScreen(
                state = state,
                name = TextFieldState("Silent Lynx 37"),
                onBack = {},
                onShuffle = {},
                onSave = {},
                onLink = {},
                onDiagnostics = {},
                onDelete = {},
            )
        }
    }

    @Test
    fun guest() = shot("profile_guest", ProfileUiState(avatarSeed = "screenshot", user = AuthUser("u", isAnonymous = true, email = null)))

    @Test
    fun linked() = shot(
        "profile_linked",
        ProfileUiState(
            avatarSeed = "screenshot",
            user = AuthUser("u", isAnonymous = false, email = "player@example.com"),
            diagnostics = true,
        ),
    )
}
