package com.arnavgames.cloakrun.feature.lobby

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.haptics.HapticCue
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.lobby.Lobby
import com.arnavgames.cloakrun.core.game.lobby.Role
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons

/**
 * The pre-game briefing: your role is revealed with a spring and a haptic, followed by the safety
 * rules. Everyone confirms it before the match can begin.
 */
@Composable
internal fun ColumnScope.Briefing(
    state: LobbyUiState,
    lobby: Lobby,
    actions: LobbyActions,
    onEndRequest: () -> Unit,
) {
    val colors = CloakTheme.colors
    val spacing = CloakTheme.spacing
    val me = state.myUid?.let(lobby::player)
    val briefed = lobby.players.count { it.briefed }
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
    ) {
        Text(
            stringResource(R.string.briefing_title),
            style = CloakTheme.typography.headline,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        if (me != null) RoleReveal(me.role)
        GlassCard(Modifier.fillMaxWidth()) {
            Rule(CloakIcons.Zone, R.string.briefing_area)
            Rule(CloakIcons.Shield, R.string.briefing_rules)
        }
        Text(
            pluralStringResource(R.plurals.briefing_progress, lobby.players.size, briefed, lobby.players.size),
            style = CloakTheme.typography.bodyStrong,
            color = colors.textSecondary,
        )
        if (lobby.everyoneBriefed) {
            Text(stringResource(R.string.briefing_all_done), style = CloakTheme.typography.body, color = colors.success)
        }
    }
    if (me != null && !me.briefed) {
        CloakButton(
            stringResource(R.string.briefing_confirm),
            onClick = actions.onConfirmBriefing,
            icon = CloakIcons.Check,
            large = true,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (me != null) {
        Text(stringResource(R.string.briefing_confirmed), style = CloakTheme.typography.bodyStrong, color = colors.success)
    }
    if (state.isHost) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
            CloakButton(
                stringResource(R.string.briefing_back),
                onClick = actions.onBackToLobby,
                style = CloakButtonStyle.Glass,
                icon = CloakIcons.Back,
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
            )
            CloakButton(
                stringResource(R.string.lobby_end),
                onClick = onEndRequest,
                style = CloakButtonStyle.Danger,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        CloakButton(stringResource(R.string.lobby_leave), onClick = actions.onLeave, style = CloakButtonStyle.Ghost, icon = CloakIcons.Back)
    }
    Spacer(Modifier)
}

@Composable
private fun RoleReveal(role: Role) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    val haptics = CloakTheme.haptics
    val scale = remember { Animatable(if (motion.reduced) 1f else 0.6f) }
    LaunchedEffect(role) {
        haptics.play(HapticCue.PowerUp)
        if (!motion.reduced) {
            scale.snapTo(0.6f)
            scale.animateTo(1f, motion.bouncy())
        }
    }
    val seeker = role == Role.Seeker
    val tint = if (seeker) colors.teamSeeker else colors.teamHider
    GlassCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s),
        ) {
            CloakIcon(if (seeker) CloakIcons.Seeker else CloakIcons.Hider, contentDescription = null, tint = tint, size = 64.dp)
            Text(stringResource(R.string.briefing_you_are), style = CloakTheme.typography.overline, color = colors.textSecondary)
            Text(stringResource(role.label()), style = CloakTheme.typography.display, color = tint)
            Text(
                stringResource(if (seeker) R.string.briefing_seeker_body else R.string.briefing_hider_body),
                style = CloakTheme.typography.body,
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun Rule(icon: androidx.compose.ui.graphics.vector.ImageVector, text: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m), verticalAlignment = Alignment.Top) {
        CloakIcon(icon, contentDescription = null, tint = CloakTheme.colors.accent, size = 22.dp)
        Text(
            stringResource(text),
            style = CloakTheme.typography.body,
            color = CloakTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}
