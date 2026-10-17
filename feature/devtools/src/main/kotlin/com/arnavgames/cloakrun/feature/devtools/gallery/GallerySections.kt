package com.arnavgames.cloakrun.feature.devtools.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.R as DesignR
import com.arnavgames.cloakrun.core.designsystem.component.CloakAvatar
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.CloakChip
import com.arnavgames.cloakrun.core.designsystem.component.CloakSlider
import com.arnavgames.cloakrun.core.designsystem.component.CloakTextField
import com.arnavgames.cloakrun.core.designsystem.component.CloakToggle
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.component.GlassSheetLayout
import com.arnavgames.cloakrun.core.designsystem.component.HoldToConfirmButton
import com.arnavgames.cloakrun.core.designsystem.component.NumberTicker
import com.arnavgames.cloakrun.core.designsystem.component.RadarLoader
import com.arnavgames.cloakrun.core.designsystem.component.SegmentedTabs
import com.arnavgames.cloakrun.core.designsystem.component.SplitFlapText
import com.arnavgames.cloakrun.core.designsystem.component.StateMessage
import com.arnavgames.cloakrun.core.designsystem.component.StatusPill
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.haptics.HapticCue
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.token.ColorMath
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons
import com.arnavgames.cloakrun.feature.devtools.R
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun BrandSection() {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(painterResource(DesignR.drawable.cr_emblem), contentDescription = null, modifier = Modifier.size(72.dp))
            Image(painterResource(DesignR.drawable.cr_wordmark), contentDescription = null, modifier = Modifier.width(200.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColourSection() {
    val c = CloakTheme.colors
    val swatches = listOf(
        "background" to c.background, "raised" to c.backgroundRaised, "primary" to c.primary,
        "primary container" to c.primaryContainer, "accent" to c.accent, "secondary" to c.secondary,
        "success" to c.success, "danger" to c.danger, "warning" to c.warning, "hider team" to c.teamHider,
        "seeker team" to c.teamSeeker, "text" to c.textPrimary, "text 2" to c.textSecondary, "text 3" to c.textTertiary,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { (name, color) ->
            val ratio = ColorMath.contrastRatio(color.toArgb() and 0xFFFFFF, c.background.toArgb() and 0xFFFFFF)
            Column(Modifier.width(100.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(color, SquircleShape(14.dp))
                        .border(1.dp, c.outline, SquircleShape(14.dp)),
                )
                Text(name, style = CloakTheme.typography.caption, color = c.textPrimary)
                Text(
                    stringResource(R.string.gallery_contrast, String.format(Locale.ROOT, "%.1f", ratio)),
                    style = CloakTheme.typography.caption,
                    color = c.textTertiary,
                )
            }
        }
    }
}

@Composable
internal fun TypeSection() {
    val t = CloakTheme.typography
    val samples: List<Pair<String, TextStyle>> = listOf(
        "Hero" to t.hero, "Display" to t.display, "Headline" to t.headline, "Title" to t.title,
        "Body: the zone is shrinking." to t.body, "Label" to t.label, "OVERLINE" to t.overline,
        "Caption" to t.caption, "12:08" to t.timer, "1,234,567" to t.numeric,
    )
    GlassCard {
        samples.forEach { (text, style) -> Text(text, style = style, color = CloakTheme.colors.textPrimary) }
    }
}

@Composable
internal fun ShapeSection() {
    val c = CloakTheme.colors
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(28.dp, 40.dp).forEach { radius ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(140.dp)) {
                        val circular = RoundedCornerShape(radius).createOutline(size, layoutDirection, this)
                        val squircle = SquircleShape(radius).createOutline(size, layoutDirection, this)
                        drawPath(Path().apply { addOutline(squircle) }, c.primary.copy(alpha = 0.35f))
                        drawPath(Path().apply { addOutline(circular) }, c.danger, style = Stroke(1.5.dp.toPx()))
                        drawPath(Path().apply { addOutline(squircle) }, c.accent, style = Stroke(2.dp.toPx()))
                    }
                    Text("${radius.value.toInt()} dp", style = CloakTheme.typography.caption, color = c.textSecondary)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.gallery_squircle), style = CloakTheme.typography.caption, color = c.accent)
            Text(stringResource(R.string.gallery_circular), style = CloakTheme.typography.caption, color = c.danger)
        }
    }
}

@Composable
internal fun GlassSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            R.string.gallery_glass_surface to GlassDefaults.surface(),
            R.string.gallery_glass_sheet to GlassDefaults.sheet(),
            R.string.gallery_glass_chrome to GlassDefaults.chrome(),
        ).forEach { (label, style) ->
            GlassSurface(shape = SquircleShape(CloakTheme.radii.card), style = style, modifier = Modifier.fillMaxWidth().height(88.dp)) {
                Text(
                    stringResource(label),
                    style = CloakTheme.typography.titleSmall,
                    color = CloakTheme.colors.textPrimary,
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 24.dp),
                )
            }
        }
        Text(stringResource(R.string.gallery_glass_hint), style = CloakTheme.typography.caption, color = CloakTheme.colors.textTertiary)
    }
}

@Composable
internal fun ButtonSection() {
    var confirmed by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CloakButton(stringResource(R.string.gallery_button_primary), onClick = {
        }, icon = CloakIcons.Play, large = true, modifier = Modifier.fillMaxWidth())
        CloakButton(stringResource(R.string.gallery_button_glass), onClick = {
        }, style = CloakButtonStyle.Glass, icon = CloakIcons.Invite, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CloakButton(stringResource(R.string.gallery_button_ghost), onClick = {
            }, style = CloakButtonStyle.Ghost, modifier = Modifier.weight(1f))
            CloakButton(stringResource(R.string.gallery_button_danger), onClick = {
            }, style = CloakButtonStyle.Danger, modifier = Modifier.weight(1f))
        }
        CloakButton(stringResource(R.string.gallery_button_disabled), onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        HoldToConfirmButton(
            text = if (confirmed) stringResource(R.string.gallery_confirmed) else stringResource(R.string.gallery_button_hold),
            icon = CloakIcons.Sos,
            onConfirm = { confirmed = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ControlSection() {
    var mode by remember { mutableIntStateOf(0) }
    var haptics by remember { mutableStateOf(true) }
    var sound by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var volume by remember { mutableFloatStateOf(0.6f) }
    val c = CloakTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple(R.string.gallery_chip_classic, CloakIcons.Radar, 0),
                Triple(R.string.gallery_chip_infection, CloakIcons.Ghost, 1),
                Triple(R.string.gallery_chip_night, CloakIcons.Moon, 2),
            ).forEach { (label, icon, i) ->
                CloakChip(stringResource(label), selected = mode == i, onClick = { mode = i }, icon = icon)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.gallery_toggle_haptics),
                style = CloakTheme.typography.body,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            CloakToggle(haptics, { haptics = it }, contentDescription = stringResource(R.string.gallery_toggle_haptics))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.gallery_toggle_sound),
                style = CloakTheme.typography.body,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            CloakToggle(sound, { sound = it }, contentDescription = stringResource(R.string.gallery_toggle_sound))
        }
        SegmentedTabs(
            options = persistentListOf(
                stringResource(R.string.gallery_tab_hider),
                stringResource(R.string.gallery_tab_seeker),
                stringResource(R.string.gallery_tab_spectator),
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.gallery_slider_volume), style = CloakTheme.typography.caption, color = c.textSecondary)
        CloakSlider(volume, { volume = it }, contentDescription = stringResource(R.string.gallery_slider_volume))
    }
}

@Composable
internal fun NumberSection() {
    var score by remember { mutableIntStateOf(1240) }
    var code by remember { mutableStateOf("K7Q2XM") }
    val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    GlassCard {
        Text(stringResource(R.string.gallery_score), style = CloakTheme.typography.overline, color = CloakTheme.colors.textSecondary)
        NumberTicker(String.format(Locale.ROOT, "%,d", score), style = CloakTheme.typography.display)
        CloakButton(stringResource(R.string.gallery_add_points), onClick = {
            score += 137
        }, style = CloakButtonStyle.Glass, icon = CloakIcons.Plus)
        Text(stringResource(R.string.gallery_join_code), style = CloakTheme.typography.overline, color = CloakTheme.colors.textSecondary)
        SplitFlapText(code)
        CloakButton(
            stringResource(R.string.gallery_new_code),
            onClick = { code = (1..6).map { alphabet.random() }.joinToString("") },
            style = CloakButtonStyle.Glass,
            icon = CloakIcons.Replay,
        )
        StatusPill(
            roleLabel = stringResource(R.string.gallery_role_hider),
            roleColor = CloakTheme.colors.teamHider,
            clock = "12:08",
            sharingLabel = stringResource(R.string.gallery_sharing_location),
        )
    }
}

@Composable
internal fun InputSection() {
    val state = rememberTextFieldState()
    CloakTextField(
        state = state,
        label = stringResource(R.string.gallery_field_label),
        placeholder = stringResource(R.string.gallery_field_placeholder),
        supportingText = stringResource(R.string.gallery_field_support),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AvatarSection() {
    val names = listOf("Nova", "Ash", "Kiran", "Mira", "Zed", "Juno", "Rio", "Tara", "Vik", "Lumi", "Oz", "Sage")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        names.forEach { CloakAvatar(seed = it, contentDescription = stringResource(R.string.gallery_avatar_cd, it), size = 52.dp) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IconSection() {
    val c = CloakTheme.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CloakIcons.all.forEach { (name, icon) ->
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .background(c.glassTint, SquircleShape(16.dp))
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CloakIcon(icon, contentDescription = name, tint = c.textPrimary)
                Text(name, style = CloakTheme.typography.caption, color = c.textTertiary, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HapticSection() {
    val haptics = CloakTheme.haptics
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HapticCue.entries.forEach { cue ->
            CloakChip(cue.name, selected = false, onClick = { haptics.play(cue) }, icon = CloakIcons.Haptic)
        }
    }
}

@Composable
internal fun StateSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard {
            StateMessage(
                icon = CloakIcons.Lobby,
                title = stringResource(R.string.gallery_empty_title),
                body = stringResource(R.string.gallery_empty_body),
            )
        }
        GlassCard {
            StateMessage(
                icon = CloakIcons.Signal,
                title = stringResource(R.string.gallery_error_title),
                body = stringResource(R.string.gallery_error_body),
                isError = true,
                action = { CloakButton(stringResource(R.string.gallery_retry), onClick = {}, style = CloakButtonStyle.Glass) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RadarLoader(size = 64.dp)
            RadarLoader(size = 64.dp, color = CloakTheme.colors.teamSeeker)
        }
    }
}

@Composable
internal fun SheetSection() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(520.dp)
            .background(Color.Transparent, SquircleShape(CloakTheme.radii.card)),
    ) {
        GlassSheetLayout(
            sheetContent = {
                Text(
                    stringResource(R.string.gallery_sheet_title),
                    style = CloakTheme.typography.title,
                    color = CloakTheme.colors.textPrimary,
                )
                Text(
                    stringResource(R.string.gallery_sheet_body),
                    style = CloakTheme.typography.bodySmall,
                    color = CloakTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
            expandLabel = stringResource(R.string.gallery_expand),
            collapseLabel = stringResource(R.string.gallery_collapse),
        ) {
            GalleryBackdrop()
        }
    }
}
