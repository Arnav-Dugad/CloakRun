package com.arnavgames.cloakrun.core.designsystem.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.arnavgames.cloakrun.core.designsystem.token.CloakBlur
import com.arnavgames.cloakrun.core.designsystem.token.CloakColors
import com.arnavgames.cloakrun.core.designsystem.token.CloakMotion
import com.arnavgames.cloakrun.core.designsystem.token.CloakRadii
import com.arnavgames.cloakrun.core.designsystem.token.CloakSizes
import com.arnavgames.cloakrun.core.designsystem.token.CloakSpacing
import com.arnavgames.cloakrun.core.designsystem.token.CloakThemeMode
import com.arnavgames.cloakrun.core.designsystem.token.CloakTypography

internal val LocalCloakColors = staticCompositionLocalOf { CloakColors.Night }
internal val LocalCloakTypography = staticCompositionLocalOf { CloakTypography.Default }
internal val LocalCloakSpacing = staticCompositionLocalOf { CloakSpacing() }
internal val LocalCloakRadii = staticCompositionLocalOf { CloakRadii() }
internal val LocalCloakSizes = staticCompositionLocalOf { CloakSizes() }
internal val LocalCloakBlur = staticCompositionLocalOf { CloakBlur() }
internal val LocalCloakMotion = staticCompositionLocalOf { CloakMotion() }

/** User preference for the theme. [System] follows the system dark setting (Night or Daylight). */
enum class ThemePreference { System, Night, Amoled, Daylight }

/**
 * Root theme for every CloakRun screen.
 *
 * @param preference the user's theme choice.
 * @param reduceMotion forces reduced motion on. When false the system "remove animations"
 *   setting still applies.
 */
@Composable
fun CloakTheme(
    preference: ThemePreference = ThemePreference.System,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val mode = when (preference) {
        ThemePreference.System -> if (systemDark) CloakThemeMode.Night else CloakThemeMode.Daylight
        ThemePreference.Night -> CloakThemeMode.Night
        ThemePreference.Amoled -> CloakThemeMode.Amoled
        ThemePreference.Daylight -> CloakThemeMode.Daylight
    }
    val colors = CloakColors.forMode(mode)
    val systemReduced = rememberSystemAnimationsDisabled()
    val motion = remember(reduceMotion, systemReduced) { CloakMotion(reduced = reduceMotion || systemReduced) }

    CompositionLocalProvider(
        LocalCloakColors provides colors,
        LocalCloakTypography provides CloakTypography.Default,
        LocalCloakMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography = CloakTypography.Default.toMaterialTypography(),
            content = content,
        )
    }
}

/** Accessors for the active Frost Glass tokens, mirroring `MaterialTheme`. */
object CloakTheme {
    val colors: CloakColors
        @Composable @ReadOnlyComposable
        get() = LocalCloakColors.current

    val typography: CloakTypography
        @Composable @ReadOnlyComposable
        get() = LocalCloakTypography.current

    val spacing: CloakSpacing
        @Composable @ReadOnlyComposable
        get() = LocalCloakSpacing.current

    val radii: CloakRadii
        @Composable @ReadOnlyComposable
        get() = LocalCloakRadii.current

    val sizes: CloakSizes
        @Composable @ReadOnlyComposable
        get() = LocalCloakSizes.current

    val blur: CloakBlur
        @Composable @ReadOnlyComposable
        get() = LocalCloakBlur.current

    val motion: CloakMotion
        @Composable @ReadOnlyComposable
        get() = LocalCloakMotion.current
}

/** Reads `ANIMATOR_DURATION_SCALE` and keeps observing it, so toggling the setting applies live. */
@Composable
private fun rememberSystemAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    var disabled by remember { mutableStateOf(context.animationsDisabled()) }
    DisposableEffect(context) {
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                disabled = context.animationsDisabled()
            }
        }
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return disabled
}

private fun Context.animationsDisabled(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private fun CloakColors.toMaterialScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primaryContainer,
        onPrimary = onPrimary,
        secondary = secondary,
        tertiary = accent,
        background = background,
        onBackground = textPrimary,
        surface = backgroundRaised,
        onSurface = textPrimary,
        surfaceVariant = backgroundRaised,
        onSurfaceVariant = textSecondary,
        error = danger,
        outline = outline,
        scrim = scrim,
    )
}

private fun CloakTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = hero,
    displayMedium = display,
    headlineMedium = headline,
    titleLarge = title,
    titleMedium = titleSmall,
    bodyLarge = body,
    bodyMedium = bodySmall,
    labelLarge = label,
    labelSmall = caption,
)
