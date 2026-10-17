package com.arnavgames.cloakrun.core.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw Frost Glass palette. Values are taken from the brand artwork (branding/cloakrun-icon.svg)
 * and extended with the accent ramps. Nothing outside this file should use hex literals.
 */
object CloakPalette {
    // Midnight base, from the icon background gradient.
    val Midnight950 = Color(0xFF05040F)
    val Midnight900 = Color(0xFF060514)
    val Midnight850 = Color(0xFF0A0820)
    val Midnight800 = Color(0xFF0E0A2B)
    val Midnight700 = Color(0xFF161040)
    val Midnight600 = Color(0xFF1D1157)
    val Midnight500 = Color(0xFF2A1D74)

    // Electric violet, from the aura and cloak gradients.
    val Violet700 = Color(0xFF3B2BB8)
    val Violet600 = Color(0xFF5A41E0)
    val Violet550 = Color(0xFF6448F0)
    val Violet500 = Color(0xFF7358F6)
    val Violet400 = Color(0xFF7C5CFF)
    val Violet300 = Color(0xFFA48CFF)
    val Violet200 = Color(0xFFBBAAFF)
    val Violet100 = Color(0xFFC9BDFF)
    val Violet50 = Color(0xFFF6F3FF)

    // Cyan signal, from the signal eye and radar arc.
    val Cyan600 = Color(0xFF007C8F)
    val Cyan500 = Color(0xFF19C8DE)
    val Cyan400 = Color(0xFF5CF2FF)
    val Cyan200 = Color(0xFFC9FBFF)

    // Magenta secondary.
    val Magenta600 = Color(0xFFB0127F)
    val Magenta400 = Color(0xFFFF4FD8)
    val Magenta200 = Color(0xFFFFB3EF)

    // Mint success.
    val Mint600 = Color(0xFF00805A)
    val Mint400 = Color(0xFF3DF5B0)

    // Coral danger.
    val Coral600 = Color(0xFFC2333B)
    val Coral700 = Color(0xFFA3122A)
    val Coral400 = Color(0xFFFF6B6B)

    // Amber: seeker team and warnings. Blue and orange is the most colour-blind-robust pairing.
    val Amber700 = Color(0xFF9A4A00)
    val Amber400 = Color(0xFFFFA43A)

    // Daylight neutrals.
    val Frost50 = Color(0xFFF7F7FD)
    val Frost100 = Color(0xFFEEEFF9)
    val Frost200 = Color(0xFFDCDDF0)
    val Ink900 = Color(0xFF120E2E)
    val Ink700 = Color(0xFF3A3560)
    val Ink500 = Color(0xFF5E5A80)

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
}

/** Which Frost Glass theme is active. */
enum class CloakThemeMode { Night, Amoled, Daylight }

/**
 * Semantic colours. Components read these, never the raw palette, so every theme stays consistent.
 *
 * Glass colours are tints laid over a blurred backdrop: [glassTint] is the 8 to 16 percent fill,
 * [glassEdgeHighlight] and [glassEdgeShade] form the 1dp gradient rim (bright top-left, faint bottom-right).
 *
 * [primary] is the brand violet used for glows, focus and accents. Filled controls use
 * [primaryContainer], which is dark enough for [onPrimary] text to meet WCAG AA.
 */
@Immutable
data class CloakColors(
    val mode: CloakThemeMode,
    val background: Color,
    val backgroundRaised: Color,
    val backgroundGlow: Color,
    val glassTint: Color,
    val glassTintStrong: Color,
    val glassEdgeHighlight: Color,
    val glassEdgeShade: Color,
    val glassShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val primaryGlow: Color,
    val accent: Color,
    val secondary: Color,
    val success: Color,
    val danger: Color,
    val warning: Color,
    val teamHider: Color,
    val teamSeeker: Color,
    val spectral: Color,
    val outline: Color,
    val scrim: Color,
) {
    val isDark: Boolean get() = mode != CloakThemeMode.Daylight

    companion object {
        val Night = CloakColors(
            mode = CloakThemeMode.Night,
            background = CloakPalette.Midnight800,
            backgroundRaised = CloakPalette.Midnight700,
            backgroundGlow = CloakPalette.Violet400.copy(alpha = 0.32f),
            glassTint = CloakPalette.White.copy(alpha = 0.08f),
            glassTintStrong = CloakPalette.White.copy(alpha = 0.16f),
            glassEdgeHighlight = CloakPalette.White.copy(alpha = 0.42f),
            glassEdgeShade = CloakPalette.White.copy(alpha = 0.06f),
            glassShadow = CloakPalette.Violet700.copy(alpha = 0.45f),
            textPrimary = CloakPalette.Violet50,
            textSecondary = CloakPalette.Violet100,
            textTertiary = CloakPalette.Violet300,
            primary = CloakPalette.Violet400,
            primaryContainer = CloakPalette.Violet550,
            onPrimary = CloakPalette.White,
            primaryGlow = CloakPalette.Violet400.copy(alpha = 0.55f),
            accent = CloakPalette.Cyan400,
            secondary = CloakPalette.Magenta400,
            success = CloakPalette.Mint400,
            danger = CloakPalette.Coral400,
            warning = CloakPalette.Amber400,
            teamHider = CloakPalette.Cyan400,
            teamSeeker = CloakPalette.Amber400,
            spectral = CloakPalette.Violet200.copy(alpha = 0.6f),
            outline = CloakPalette.White.copy(alpha = 0.14f),
            scrim = CloakPalette.Midnight950.copy(alpha = 0.72f),
        )

        /** True-black variant for OLED panels: identical accents, black base, slightly brighter glass. */
        val Amoled = Night.copy(
            mode = CloakThemeMode.Amoled,
            background = CloakPalette.Black,
            backgroundRaised = CloakPalette.Midnight900,
            backgroundGlow = CloakPalette.Violet400.copy(alpha = 0.22f),
            glassTint = CloakPalette.White.copy(alpha = 0.10f),
            glassTintStrong = CloakPalette.White.copy(alpha = 0.16f),
            scrim = CloakPalette.Black.copy(alpha = 0.8f),
        )

        /** Sunlight-readable glass. Accents are darkened so text on the light base keeps WCAG AA. */
        val Daylight = CloakColors(
            mode = CloakThemeMode.Daylight,
            background = CloakPalette.Frost100,
            backgroundRaised = CloakPalette.Frost50,
            backgroundGlow = CloakPalette.Violet300.copy(alpha = 0.35f),
            glassTint = CloakPalette.White.copy(alpha = 0.55f),
            glassTintStrong = CloakPalette.White.copy(alpha = 0.72f),
            glassEdgeHighlight = CloakPalette.White.copy(alpha = 0.95f),
            glassEdgeShade = CloakPalette.Ink900.copy(alpha = 0.08f),
            glassShadow = CloakPalette.Violet600.copy(alpha = 0.22f),
            textPrimary = CloakPalette.Ink900,
            textSecondary = CloakPalette.Ink700,
            textTertiary = CloakPalette.Ink500,
            primary = CloakPalette.Violet600,
            primaryContainer = CloakPalette.Violet600,
            onPrimary = CloakPalette.White,
            primaryGlow = CloakPalette.Violet500.copy(alpha = 0.35f),
            accent = CloakPalette.Cyan600,
            secondary = CloakPalette.Magenta600,
            success = CloakPalette.Mint600,
            danger = CloakPalette.Coral700,
            warning = CloakPalette.Amber700,
            teamHider = CloakPalette.Cyan600,
            teamSeeker = CloakPalette.Amber700,
            spectral = CloakPalette.Violet600.copy(alpha = 0.5f),
            outline = CloakPalette.Ink900.copy(alpha = 0.12f),
            scrim = CloakPalette.Ink900.copy(alpha = 0.45f),
        )

        fun forMode(mode: CloakThemeMode): CloakColors = when (mode) {
            CloakThemeMode.Night -> Night
            CloakThemeMode.Amoled -> Amoled
            CloakThemeMode.Daylight -> Daylight
        }
    }
}
