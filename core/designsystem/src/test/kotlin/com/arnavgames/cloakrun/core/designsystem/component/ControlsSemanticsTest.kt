package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TalkBack must hear these controls as what they are, with their current state. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ControlsSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun toggle_isASwitchWithItsLabelAndState() {
        compose.setContent {
            CloakTheme {
                var on by remember { mutableStateOf(false) }
                CloakToggle(checked = on, onCheckedChange = { on = it }, contentDescription = "Haptics")
            }
        }
        val node = compose.onNodeWithContentDescription("Haptics")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        node.assertIsOff()
        node.performClick()
        node.assertIsOn()
    }

    @Test
    fun slider_exposesRangeAndCanBeSet() {
        compose.setContent {
            CloakTheme {
                var v by remember { mutableFloatStateOf(0.25f) }
                CloakSlider(value = v, onValueChange = { v = it }, contentDescription = "Volume")
            }
        }
        compose.onNodeWithContentDescription("Volume")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
    }
}
