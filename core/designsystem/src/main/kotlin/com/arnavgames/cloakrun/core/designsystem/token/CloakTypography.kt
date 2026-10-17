package com.arnavgames.cloakrun.core.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.arnavgames.cloakrun.core.designsystem.R

/**
 * Bundled open-licence variable fonts (SIL OFL 1.1, licence texts in docs/licenses):
 * Sora for display (geometric, wght 100 to 800) and Inter for text (opsz 14 to 32, wght 100 to 900).
 */
private fun sora(weight: Int) = Font(
    resId = R.font.sora_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun inter(weight: Int, opticalSize: TextUnit) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.opticalSizing(opticalSize),
    ),
)

val SoraFamily = FontFamily(
    sora(400),
    sora(500),
    sora(600),
    sora(700),
    sora(800),
)

/** Inter tuned for text sizes (opsz 16). */
val InterFamily = FontFamily(
    inter(400, 16.sp),
    inter(500, 16.sp),
    inter(600, 16.sp),
    inter(700, 16.sp),
)

/** Inter tuned for large figures (opsz 32): tighter spacing, finer details. */
val InterDisplayFamily = FontFamily(
    inter(500, 32.sp),
    inter(700, 32.sp),
)

/** OpenType feature string for tabular (fixed-width) figures. Timers and scores must not jitter. */
const val TABULAR_FIGURES = "tnum"

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

@Immutable
data class CloakTypography(
    val hero: TextStyle,
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val overline: TextStyle,
    val caption: TextStyle,
    val timer: TextStyle,
    val countdown: TextStyle,
    val numeric: TextStyle,
) {
    companion object {
        val Default = CloakTypography(
            hero = TextStyle(
                fontFamily = SoraFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 56.sp,
                lineHeight = 60.sp,
                letterSpacing = (-0.02).em,
                lineHeightStyle = Trim,
            ),
            display = TextStyle(
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 46.sp,
                letterSpacing = (-0.015).em,
                lineHeightStyle = Trim,
            ),
            headline = TextStyle(
                fontFamily = SoraFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.01).em,
            ),
            title = TextStyle(
                fontFamily = SoraFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = SoraFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
            body = TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            bodyStrong = TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            label = TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.01.em,
            ),
            overline = TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.18.em,
            ),
            caption = TextStyle(
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
            timer = TextStyle(
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontFeatureSettings = TABULAR_FIGURES,
                lineHeightStyle = Trim,
            ),
            countdown = TextStyle(
                fontFamily = SoraFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 160.sp,
                lineHeight = 160.sp,
                letterSpacing = (-0.04).em,
                fontFeatureSettings = TABULAR_FIGURES,
                lineHeightStyle = Trim,
            ),
            numeric = TextStyle(
                fontFamily = InterDisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
        )
    }
}
