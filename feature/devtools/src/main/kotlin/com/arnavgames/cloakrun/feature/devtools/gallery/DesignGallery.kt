package com.arnavgames.cloakrun.feature.devtools.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.component.CloakToggle
import com.arnavgames.cloakrun.core.designsystem.component.SegmentedTabs
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import com.arnavgames.cloakrun.feature.devtools.R
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class GallerySettings(
    val theme: ThemePreference,
    val reduceMotion: Boolean,
    val performanceMode: Boolean,
)

/** Every gallery section, in order. Screenshot tests iterate this list. */
internal val GallerySections: List<Pair<Int, @Composable () -> Unit>> = listOf(
    R.string.gallery_section_brand to { BrandSection() },
    R.string.gallery_section_colours to { ColourSection() },
    R.string.gallery_section_type to { TypeSection() },
    R.string.gallery_section_shapes to { ShapeSection() },
    R.string.gallery_section_glass to { GlassSection() },
    R.string.gallery_section_buttons to { ButtonSection() },
    R.string.gallery_section_controls to { ControlSection() },
    R.string.gallery_section_numbers to { NumberSection() },
    R.string.gallery_section_inputs to { InputSection() },
    R.string.gallery_section_avatars to { AvatarSection() },
    R.string.gallery_section_icons to { IconSection() },
    R.string.gallery_section_haptics to { HapticSection() },
    R.string.gallery_section_states to { StateSection() },
    R.string.gallery_section_sheet to { SheetSection() },
)

@Composable
fun DesignGallery(
    settings: GallerySettings,
    onThemeChange: (ThemePreference) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onPerformanceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themes = remember { listOf(ThemePreference.Night, ThemePreference.Amoled, ThemePreference.Daylight) }
    GlassScene(backdrop = { GalleryBackdrop() }, modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.xl),
        ) {
            item {
                Column(
                    Modifier.padding(horizontal = CloakTheme.spacing.gutter, vertical = CloakTheme.spacing.l),
                    verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m),
                ) {
                    Text(
                        stringResource(R.string.gallery_title),
                        style = CloakTheme.typography.display,
                        color = CloakTheme.colors.textPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.gallery_subtitle),
                        style = CloakTheme.typography.body,
                        color = CloakTheme.colors.textSecondary,
                    )
                    SegmentedTabs(
                        options = persistentListOf(
                            stringResource(R.string.gallery_theme_night),
                            stringResource(R.string.gallery_theme_amoled),
                            stringResource(R.string.gallery_theme_daylight),
                        ),
                        selectedIndex = themes.indexOf(settings.theme).coerceAtLeast(0),
                        onSelect = { onThemeChange(themes[it]) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SettingRow(stringResource(R.string.gallery_reduce_motion), settings.reduceMotion, onReduceMotionChange)
                    SettingRow(stringResource(R.string.gallery_performance_mode), settings.performanceMode, onPerformanceChange)
                }
            }
            GallerySections.forEach { (title, content) ->
                item(key = title) {
                    Column(Modifier.padding(horizontal = CloakTheme.spacing.gutter)) {
                        SectionTitle(stringResource(title))
                        content()
                    }
                }
            }
            item { Spacer(Modifier.height(CloakTheme.spacing.huge)) }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = CloakTheme.typography.bodyStrong, color = CloakTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        CloakToggle(checked = checked, onCheckedChange = onChange, contentDescription = label)
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = CloakTheme.typography.overline,
        color = CloakTheme.colors.accent,
        modifier = Modifier.padding(bottom = CloakTheme.spacing.m).semantics { heading() },
    )
}

/**
 * A busy, map-like backdrop (jittered street grid, glowing arterials, a park) so that glass
 * blur, tint and contrast can be judged against something realistic.
 */
@Composable
internal fun GalleryBackdrop(modifier: Modifier = Modifier) {
    val colors = CloakTheme.colors
    val streets = remember { backdropStreets() }
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(colors.background)
            val scale = max(size.width, size.height) / 1000f
            drawCircle(
                Brush.radialGradient(
                    listOf(colors.success.copy(alpha = 0.22f), colors.success.copy(alpha = 0f)),
                    center = Offset(size.width * 0.7f, size.height * 0.25f),
                    radius = 220f * scale,
                ),
                radius = 220f * scale,
                center = Offset(size.width * 0.7f, size.height * 0.25f),
            )
            val minor = Stroke(width = 1.4f * scale)
            for (p in streets.minor) drawPath(p.scaled(scale), colors.primary.copy(alpha = 0.22f), style = minor)
            for (p in streets.major) {
                val path = p.scaled(scale)
                drawPath(path, colors.accent.copy(alpha = 0.18f), style = Stroke(width = 10f * scale))
                drawPath(path, colors.accent.copy(alpha = 0.7f), style = Stroke(width = 2.2f * scale))
            }
            val glowCenter = Offset(size.width * 0.2f, size.height * 0.1f)
            val glowRadius = size.maxDimension * 0.6f
            drawCircle(
                Brush.radialGradient(
                    listOf(colors.backgroundGlow, colors.backgroundGlow.copy(alpha = 0f)),
                    center = glowCenter,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = glowCenter,
            )
        }
    }
}

private class Streets(val minor: List<List<Offset>>, val major: List<List<Offset>>)

private fun List<Offset>.scaled(scale: Float): Path = Path().apply {
    forEachIndexed { i, o -> if (i == 0) moveTo(o.x * scale, o.y * scale) else lineTo(o.x * scale, o.y * scale) }
}

private fun backdropStreets(): Streets {
    val r = Random(42)
    val minor = mutableListOf<List<Offset>>()
    for (i in 0..24) {
        val x = i * 45f + r.nextFloat() * 10
        minor += listOf(Offset(x, 0f), Offset(x + r.nextFloat() * 30 - 15, 2400f))
        val y = i * 100f + r.nextFloat() * 20
        minor += listOf(Offset(0f, y), Offset(1100f, y + r.nextFloat() * 40 - 20))
    }
    val major = (0 until 4).map { k ->
        val y0 = 150f + k * 520f
        (0..60).map { s -> Offset(s * 20f, y0 + sin(s / 7f + k) * 60f) }
    }
    return Streets(minor, major)
}
