package com.arnavgames.cloakrun.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.R as DesignR
import com.arnavgames.cloakrun.core.designsystem.component.CloakAvatar
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.CloakWordmark
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.component.NightBackdrop
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons

/** The player's identity for the profile chip, and whether they are in a lobby. */
data class HomePlayer(
    val name: String,
    val avatarSeed: String,
    val inLobby: Boolean = false,
)

class HomeActions(
    val onDesignArea: () -> Unit = {},
    val onAbout: () -> Unit = {},
    val onHost: () -> Unit = {},
    val onJoin: () -> Unit = {},
    val onReturnToLobby: () -> Unit = {},
    val onProfile: () -> Unit = {},
)

/**
 * The home screen. Primary actions sit in the bottom half for one-handed reach; the emblem and
 * radar backdrop carry the brand at the top.
 */
@Composable
fun HomeScreen(
    actions: HomeActions,
    modifier: Modifier = Modifier,
    player: HomePlayer? = null,
    banner: @Composable () -> Unit = {},
) {
    val spacing = CloakTheme.spacing
    val colors = CloakTheme.colors
    GlassScene(backdrop = { NightBackdrop(focus = Offset(0.5f, 0.24f)) }, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.l)
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            if (player != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ProfileChip(player, actions.onProfile) }
            } else {
                Spacer(Modifier.height(spacing.xl))
            }
            HomeEmblem()
            Text(
                stringResource(R.string.home_tagline),
                style = CloakTheme.typography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.xl))
            banner()
            Spacer(Modifier.height(spacing.l))
            PlayCard(player?.inLobby == true, actions)
            GlassCard(Modifier.fillMaxWidth()) {
                CloakIcon(CloakIcons.Zone, contentDescription = null, tint = colors.accent, size = 32.dp)
                Text(
                    stringResource(R.string.home_design_area),
                    style = CloakTheme.typography.title,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.home_design_area_body), style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
                Spacer(Modifier.height(spacing.s))
                CloakButton(
                    stringResource(R.string.home_design_area),
                    onClick = actions.onDesignArea,
                    style = CloakButtonStyle.Glass,
                    icon = CloakIcons.Map,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.home_safety_title),
                    style = CloakTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.home_safety_body), style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
            }
            CloakButton(
                stringResource(R.string.home_about),
                onClick = actions.onAbout,
                style = CloakButtonStyle.Ghost,
                icon = CloakIcons.Info,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.home_made_by), style = CloakTheme.typography.overline, color = colors.accent)
        }
    }
}

@Composable
private fun PlayCard(inLobby: Boolean, actions: HomeActions) {
    val colors = CloakTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        CloakIcon(CloakIcons.Lobby, contentDescription = null, tint = colors.primary, size = 32.dp)
        Text(
            stringResource(R.string.home_play_title),
            style = CloakTheme.typography.title,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.home_play_body), style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(CloakTheme.spacing.s))
        if (inLobby) {
            CloakButton(
                stringResource(R.string.home_return_to_lobby),
                onClick = actions.onReturnToLobby,
                icon = CloakIcons.Lobby,
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            CloakButton(
                stringResource(R.string.home_host),
                onClick = actions.onHost,
                icon = CloakIcons.Plus,
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CloakButton(
                stringResource(R.string.home_join),
                onClick = actions.onJoin,
                style = CloakButtonStyle.Glass,
                icon = CloakIcons.Qr,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileChip(player: HomePlayer, onClick: () -> Unit) {
    val label = stringResource(R.string.home_profile_cd, player.name)
    GlassSurface(
        shape = SquircleShape.Pill,
        style = GlassDefaults.chrome(),
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
            }
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CloakAvatar(seed = player.avatarSeed, contentDescription = null, size = 36.dp)
            Text(
                player.name,
                style = CloakTheme.typography.label,
                color = CloakTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        }
    }
}

@Composable
private fun HomeEmblem() {
    val motion = CloakTheme.motion
    val scale = remember { Animatable(if (motion.reduced) 1f else 0.85f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, motion.bouncy()) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.l)) {
        Image(
            painter = painterResource(DesignR.drawable.cr_emblem),
            contentDescription = stringResource(R.string.home_cd_emblem),
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
        CloakWordmark(contentDescription = null, modifier = Modifier.width(220.dp))
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    CloakTheme(preference = ThemePreference.Night) {
        HomeScreen(actions = HomeActions(), player = HomePlayer("Silent Fox 42", "preview"))
    }
}
