package com.arnavgames.cloakrun.feature.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arnavgames.cloakrun.core.designsystem.component.NightBackdrop
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
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
class UpdateSheetScreenshotTest(private val name: String, private val state: UpdateState) {
    @Test
    fun sheet() {
        captureRoboImage(
            filePath = "src/test/screenshots/update_$name.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f)),
        ) {
            CloakTheme(reduceMotion = true) {
                GlassScene(backdrop = { NightBackdrop() }, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        UpdateSheet(state = state, release = RELEASE, onUpdate = {}, onInstall = {}, onAllow = {}, onDismiss = {})
                    }
                }
            }
        }
    }

    companion object {
        val RELEASE = ReleaseInfo(
            version = SemVer(0, 4, 0),
            tag = "v0.4.0",
            notes = "### Added\n\n- In-app updates with live progress, verified by checksum and signature.\n" +
                "- Safety cutouts for roads, water and restricted land.\n\n### Commits\n- feat: something (abc123)\n",
            publishedAt = "2026-10-17T15:00:00Z",
            apkName = "CloakRun-v0.4.0.apk",
            apkUrl = "https://example.invalid/CloakRun-v0.4.0.apk",
            apkSizeBytes = 44_100_000,
            apkServerSha256 = null,
            checksumsUrl = null,
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params(): List<Array<Any>> = listOf(
            arrayOf("available", UpdateState.Available(RELEASE)),
            arrayOf("downloading", UpdateState.Downloading(RELEASE, 18_500_000, 44_100_000)),
            arrayOf("verifying", UpdateState.Verifying(RELEASE)),
            arrayOf("ready", UpdateState.ReadyToInstall(RELEASE)),
            arrayOf("permission", UpdateState.NeedsInstallPermission(RELEASE)),
            arrayOf("failed_checksum", UpdateState.Failed(UpdateFailure.Checksum, RELEASE)),
        )
    }
}
