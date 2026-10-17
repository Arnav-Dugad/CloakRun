package com.arnavgames.cloakrun.core.designsystem.token

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.arnavgames.cloakrun.core.designsystem.token.ColorMath.Deficiency
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Guards the palette against regressions: text contrast must meet WCAG AA on every theme's
 * base, and the two team colours must stay distinguishable under every common colour-vision
 * deficiency (they are also always paired with distinct shapes on the map).
 */
class PaletteAccessibilityTest {
    private val themes = listOf(CloakColors.Night, CloakColors.Amoled, CloakColors.Daylight)

    private fun Color.rgb(): Int = toArgb() and 0xFFFFFF

    /** Glass surfaces are translucent; text is checked against the tint composited over the base. */
    private fun CloakColors.glassOverBase(): Int =
        ColorMath.over(glassTintStrong.copy(alpha = 1f).rgb(), glassTintStrong.alpha.toDouble(), background.rgb())

    @Test
    fun primaryAndSecondaryText_meetWcagAA_onBackgroundAndGlass() {
        for (theme in themes) {
            for (surface in listOf(theme.background.rgb(), theme.glassOverBase())) {
                val primary = ColorMath.contrastRatio(theme.textPrimary.rgb(), surface)
                val secondary = ColorMath.contrastRatio(theme.textSecondary.rgb(), surface)
                assertWithMessage("${theme.mode} textPrimary contrast").that(primary).isAtLeast(7.0)
                assertWithMessage("${theme.mode} textSecondary contrast").that(secondary).isAtLeast(4.5)
            }
        }
    }

    @Test
    fun tertiaryText_meetsLargeTextContrast() {
        for (theme in themes) {
            val ratio = ColorMath.contrastRatio(theme.textTertiary.rgb(), theme.background.rgb())
            assertWithMessage("${theme.mode} textTertiary contrast").that(ratio).isAtLeast(3.0)
        }
    }

    @Test
    fun onPrimary_isReadableOnPrimaryButtons() {
        for (theme in themes) {
            val ratio = ColorMath.contrastRatio(theme.onPrimary.rgb(), theme.primaryContainer.rgb())
            assertWithMessage("${theme.mode} onPrimary contrast").that(ratio).isAtLeast(4.5)
        }
    }

    @Test
    fun signalColours_areVisibleAgainstTheBase() {
        for (theme in themes) {
            for ((name, c) in listOf(
                "accent" to theme.accent,
                "success" to theme.success,
                "danger" to theme.danger,
                "warning" to theme.warning,
                "teamHider" to theme.teamHider,
                "teamSeeker" to theme.teamSeeker,
            )) {
                val ratio = ColorMath.contrastRatio(c.rgb(), theme.background.rgb())
                assertWithMessage("${theme.mode} $name vs background").that(ratio).isAtLeast(3.0)
            }
        }
    }

    @Test
    fun teamColours_stayDistinct_underEveryDeficiency() {
        for (theme in themes) {
            for (deficiency in Deficiency.entries) {
                val hider = ColorMath.simulate(theme.teamHider.rgb(), deficiency)
                val seeker = ColorMath.simulate(theme.teamSeeker.rgb(), deficiency)
                assertWithMessage("${theme.mode} team colours under $deficiency (deltaE)")
                    .that(ColorMath.deltaE(hider, seeker))
                    .isAtLeast(25.0)
            }
        }
    }

    @Test
    fun successAndDanger_stayDistinct_underEveryDeficiency() {
        for (theme in themes) {
            for (deficiency in Deficiency.entries) {
                val ok = ColorMath.simulate(theme.success.rgb(), deficiency)
                val bad = ColorMath.simulate(theme.danger.rgb(), deficiency)
                assertWithMessage("${theme.mode} success vs danger under $deficiency (deltaE)")
                    .that(ColorMath.deltaE(ok, bad))
                    .isAtLeast(15.0)
            }
        }
    }

    @Test
    fun colorMath_matchesKnownReferenceValues() {
        assertWithMessage("black on white").that(ColorMath.contrastRatio(0x000000, 0xFFFFFF)).isWithin(0.01).of(21.0)
        assertWithMessage("same colour").that(ColorMath.contrastRatio(0x777777, 0x777777)).isWithin(0.001).of(1.0)
        assertWithMessage("white lightness").that(ColorMath.lightness(0xFFFFFF)).isWithin(0.1).of(100.0)
        assertWithMessage("grey is unchanged by simulation")
            .that(ColorMath.deltaE(ColorMath.simulate(0x808080, Deficiency.Deuteranopia), 0x808080))
            .isLessThan(1.5)
    }
}
