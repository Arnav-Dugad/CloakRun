package com.arnavgames.cloakrun.feature.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.component.GlassModal
import com.arnavgames.cloakrun.core.designsystem.component.HoldToConfirmButton
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.lobby.InviteLink
import com.arnavgames.cloakrun.core.game.lobby.Lobby
import com.arnavgames.cloakrun.core.game.lobby.LobbyPlayer
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.icons.CloakIcons

@Composable
internal fun InviteModal(
    lobby: Lobby,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    GlassModal(visible = visible, onDismiss = onDismiss, title = stringResource(R.string.lobby_invite_title)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            QrCode(
                text = InviteLink.forCode(lobby.code),
                contentDescription = stringResource(R.string.lobby_qr_cd, lobby.code),
                size = 248.dp,
            )
        }
        Text(
            lobby.code.toList().joinToString(" "),
            style = CloakTheme.typography.countdown.copy(fontSize = CloakTheme.typography.headline.fontSize),
            color = CloakTheme.colors.textPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(stringResource(R.string.lobby_invite_body), style = CloakTheme.typography.bodySmall, color = CloakTheme.colors.textSecondary)
    }
}

@Composable
internal fun SettingsModal(
    config: MatchConfig,
    visible: Boolean,
    onDismiss: () -> Unit,
    onChange: (MatchConfig) -> Unit,
) {
    GlassModal(visible = visible, onDismiss = onDismiss, title = stringResource(R.string.lobby_settings)) {
        ChoiceRow(R.string.host_max_players, listOf(4, 6, 8), config.maxPlayers, { stringResource(R.string.host_up_to, it) }) {
            onChange(config.copy(maxPlayers = it, seekers = config.seekers.coerceAtMost(it - 1)))
        }
        ChoiceRow(R.string.host_seekers, (1..3).filter { it < config.maxPlayers }, config.seekers, { it.toString() }) {
            onChange(config.copy(seekers = it))
        }
        ChoiceRow(R.string.host_head_start, listOf(60, 120, 300), config.headStartSeconds, {
            stringResource(R.string.host_minutes, it / 60)
        }) {
            onChange(config.copy(headStartSeconds = it))
        }
        ChoiceRow(R.string.host_hunt, listOf(10, 20, 30, 45), config.huntMinutes, { stringResource(R.string.host_minutes, it) }) {
            onChange(config.copy(huntMinutes = it))
        }
    }
}

@Composable
internal fun KickModal(
    target: LobbyPlayer?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    GlassModal(visible = target != null, onDismiss = onDismiss, title = stringResource(R.string.lobby_kick_title, target?.name.orEmpty())) {
        Text(stringResource(R.string.lobby_kick_body), style = CloakTheme.typography.body, color = CloakTheme.colors.textSecondary)
        HoldToConfirmButton(
            text = stringResource(R.string.lobby_kick_confirm),
            icon = CloakIcons.Close,
            onConfirm = { target?.let { onConfirm(it.uid) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun EndLobbyModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    GlassModal(visible = visible, onDismiss = onDismiss, title = stringResource(R.string.lobby_end_title)) {
        Text(stringResource(R.string.lobby_end_body), style = CloakTheme.typography.body, color = CloakTheme.colors.textSecondary)
        HoldToConfirmButton(
            text = stringResource(R.string.lobby_end_confirm),
            icon = CloakIcons.Close,
            onConfirm = {
                onDismiss()
                onConfirm()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A short floating message at the top of the screen. */
@Composable
internal fun Notice(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    GlassSurface(
        shape = SquircleShape.Pill,
        style = GlassDefaults.chrome(),
        modifier = modifier.padding(top = 64.dp).padding(horizontal = CloakTheme.spacing.gutter).widthIn(max = 520.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text,
                style = CloakTheme.typography.bodyStrong,
                color = if (isError) CloakTheme.colors.danger else CloakTheme.colors.textPrimary,
            )
        }
    }
}
