package com.arnavgames.cloakrun.feature.lobby

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.component.CloakAvatar
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.component.SplitFlapText
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.lobby.Lobby
import com.arnavgames.cloakrun.core.game.lobby.LobbyPlayer
import com.arnavgames.cloakrun.core.game.lobby.Role
import com.arnavgames.cloakrun.core.game.lobby.StartBlocker
import com.arnavgames.cloakrun.core.game.lobby.StartCheck
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun ColumnScope.LobbyRoom(
    state: LobbyUiState,
    lobby: Lobby,
    actions: LobbyActions,
    dialogs: LobbyDialogs,
) {
    val isHost = state.isHost
    TopRow(
        isHost = isHost,
        onLeave = { if (isHost) dialogs.confirmEnd = true else actions.onLeave() },
        onInvite = { dialogs.invite = true },
    )
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
        item(key = "orbit") { Orbit(lobby, state.online, onOrbClick = { dialogs.invite = true }) }
        item(key = "invite") {
            InviteRow(onQr = { dialogs.invite = true }, onShare = { actions.onShare(lobby) }, onCopy = { actions.onCopy(lobby) })
        }
        item(key = "settings") { SettingsSummary(lobby, isHost, onEdit = { dialogs.settings = true }) }
        item(key = "header") {
            Text(
                stringResource(R.string.lobby_players, lobby.players.size, lobby.config.maxPlayers),
                style = CloakTheme.typography.overline,
                color = CloakTheme.colors.textSecondary,
                modifier = Modifier.padding(top = CloakTheme.spacing.s),
            )
        }
        items(lobby.ordered, key = { it.uid }) { player ->
            PlayerRow(
                player = player,
                isMe = player.uid == state.myUid,
                isHostPlayer = player.uid == lobby.hostUid,
                online = player.uid in state.online,
                hostControls = isHost,
                onCycleRole = { actions.onCycleRole(player.uid) },
                onKick = { dialogs.kick = player },
                modifier = Modifier.animateItem(),
            )
        }
    }
    BottomActions(state, lobby, actions)
}

/** Which modal is open. Modals render at the screen root so their scrim covers everything. */
@androidx.compose.runtime.Stable
internal class LobbyDialogs {
    var invite by mutableStateOf(false)
    var settings by mutableStateOf(false)
    var kick by mutableStateOf<LobbyPlayer?>(null)
    var confirmEnd by mutableStateOf(false)
}

@Composable
private fun TopRow(
    isHost: Boolean,
    onLeave: () -> Unit,
    onInvite: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CloakButton(
            stringResource(if (isHost) R.string.lobby_end else R.string.lobby_leave),
            onClick = onLeave,
            style = CloakButtonStyle.Ghost,
            icon = CloakIcons.Back,
        )
        Spacer(Modifier.weight(1f))
        CloakButton(stringResource(R.string.lobby_invite), onClick = onInvite, style = CloakButtonStyle.Glass, icon = CloakIcons.Invite)
    }
}

/**
 * The join-code orb with everyone orbiting it. New players spring in, and when someone joins or
 * leaves the others glide to their new slots. The orbit turns slowly; it stops with reduced motion.
 */
@Composable
private fun Orbit(
    lobby: Lobby,
    online: Set<String>,
    onOrbClick: () -> Unit,
) {
    val reduced = CloakTheme.motion.reduced
    val spin = if (reduced) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "orbit").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(ORBIT_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "spin",
        )
    }
    val players = lobby.ordered
    Box(Modifier.fillMaxWidth().height(ORBIT_HEIGHT), contentAlignment = Alignment.Center) {
        players.forEachIndexed { index, player ->
            OrbitingAvatar(player, slot = 360f * index / players.size, spin = { spin.value }, dim = player.uid !in online)
        }
        CodeOrb(lobby.code, onOrbClick)
    }
}

@Composable
private fun OrbitingAvatar(
    player: LobbyPlayer,
    slot: Float,
    spin: () -> Float,
    dim: Boolean,
) {
    val motion = CloakTheme.motion
    val angle by animateFloatAsState(slot, motion.bouncy(), label = "slot")
    val scale = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, motion.bouncy()) }
    CloakAvatar(
        seed = player.avatarSeed,
        contentDescription = player.name,
        size = AVATAR_SIZE,
        modifier = Modifier
            .offset {
                val a = ((angle + spin()) - 90f) * PI.toFloat() / 180f
                val r = ORBIT_RADIUS.toPx()
                IntOffset((cos(a) * r).roundToInt(), (sin(a) * r * ORBIT_TILT).roundToInt())
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = if (dim) 0.45f else 1f
            },
    )
}

@Composable
private fun CodeOrb(code: String, onClick: () -> Unit) {
    val colors = CloakTheme.colors
    val label = stringResource(R.string.lobby_code_cd, code.toList().joinToString(" "))
    GlassSurface(
        shape = SquircleShape.Pill,
        style = GlassDefaults.surface(),
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = SemanticsRole.Button
            }
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CloakIcon(CloakIcons.Qr, contentDescription = null, tint = colors.accent, size = 20.dp)
            SplitFlapText(code, tileWidth = 22.dp, contentDescription = label)
        }
    }
}

@Composable
private fun InviteRow(
    onQr: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
        InviteTile(CloakIcons.Qr, stringResource(R.string.lobby_qr), onQr, Modifier.weight(1f))
        InviteTile(CloakIcons.Share, stringResource(R.string.lobby_share), onShare, Modifier.weight(1f))
        InviteTile(CloakIcons.Copy, stringResource(R.string.lobby_copy), onCopy, Modifier.weight(1f))
    }
}

@Composable
private fun InviteTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = CloakTheme.haptics
    GlassSurface(
        shape = SquircleShape(CloakTheme.radii.m),
        style = GlassDefaults.chrome(),
        modifier = modifier
            .semantics(mergeDescendants = true) { role = SemanticsRole.Button }
            .clickable {
                haptics.play(com.arnavgames.cloakrun.core.designsystem.haptics.HapticCue.Tick)
                onClick()
            },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CloakIcon(icon, contentDescription = null, tint = CloakTheme.colors.accent, size = 22.dp)
            Text(label, style = CloakTheme.typography.label, color = CloakTheme.colors.textPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun SettingsSummary(
    lobby: Lobby,
    isHost: Boolean,
    onEdit: () -> Unit,
) {
    val c = lobby.config
    GlassCard(Modifier.fillMaxWidth(), onClick = if (isHost) onEdit else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m)) {
            CloakIcon(CloakIcons.Timer, contentDescription = null, tint = CloakTheme.colors.accent)
            Column(Modifier.weight(1f)) {
                Text(
                    "${stringResource(R.string.lobby_settings)}  ${areaSize(lobby.area)}",
                    style = CloakTheme.typography.overline,
                    color = CloakTheme.colors.textSecondary,
                )
                Text(
                    pluralStringResource(R.plurals.lobby_settings_summary, c.seekers, c.seekers, c.headStartSeconds / 60, c.huntMinutes),
                    style = CloakTheme.typography.bodyStrong,
                    color = CloakTheme.colors.textPrimary,
                )
            }
            if (isHost) CloakIcon(CloakIcons.Settings, contentDescription = null, tint = CloakTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun PlayerRow(
    player: LobbyPlayer,
    isMe: Boolean,
    isHostPlayer: Boolean,
    online: Boolean,
    hostControls: Boolean,
    onCycleRole: () -> Unit,
    onKick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CloakTheme.colors
    GlassSurface(
        shape = SquircleShape(CloakTheme.radii.m),
        style = GlassDefaults.chrome(),
        elevated = false,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = CloakTheme.spacing.m, vertical = CloakTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m),
        ) {
            CloakAvatar(
                seed = player.avatarSeed,
                contentDescription = null,
                size = 44.dp,
                modifier = Modifier.graphicsLayer {
                    alpha =
                        if (online) 1f else 0.5f
                },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    player.name,
                    style = CloakTheme.typography.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    statusLine(isMe, isHostPlayer, online, player.ready),
                    style = CloakTheme.typography.caption,
                    color = colors.textSecondary,
                )
            }
            RoleChip(player, enabled = hostControls, onClick = onCycleRole)
            if (player.ready) CloakIcon(CloakIcons.Check, contentDescription = stringResource(R.string.lobby_ready), tint = colors.success)
            if (hostControls && !isMe) {
                val label = stringResource(R.string.lobby_kick, player.name)
                Box(
                    Modifier
                        .size(CloakTheme.sizes.minTouchTarget)
                        .semantics {
                            contentDescription = label
                            role = SemanticsRole.Button
                        }
                        .clickable(onClick = onKick),
                    contentAlignment = Alignment.Center,
                ) {
                    CloakIcon(CloakIcons.Close, contentDescription = null, tint = colors.textSecondary, size = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun statusLine(
    isMe: Boolean,
    isHost: Boolean,
    online: Boolean,
    ready: Boolean,
): String = buildList {
    if (isMe) add(stringResource(R.string.lobby_you))
    if (isHost) add(stringResource(R.string.lobby_host))
    if (!online) add(stringResource(R.string.lobby_offline))
    if (!isHost) add(stringResource(if (ready) R.string.lobby_ready else R.string.lobby_not_ready))
}.joinToString("  ·  ")

@Composable
private fun RoleChip(
    player: LobbyPlayer,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CloakTheme.colors
    val tint = when (player.role) {
        Role.Hider -> colors.teamHider
        Role.Seeker -> colors.teamSeeker
        else -> colors.textTertiary
    }
    val icon = if (player.role == Role.Seeker) CloakIcons.Seeker else CloakIcons.Hider
    val roleName = stringResource(player.role.label())
    val label = stringResource(R.string.lobby_role_change_cd, player.name, roleName)
    Row(
        Modifier
            .background(tint.copy(alpha = 0.16f), SquircleShape.Pill)
            .then(
                if (enabled) {
                    Modifier
                        .semantics {
                            contentDescription = label
                            role = SemanticsRole.Button
                        }
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (player.role != Role.Unassigned) CloakIcon(icon, contentDescription = null, tint = tint, size = 16.dp)
        Text(roleName, style = CloakTheme.typography.label, color = tint)
    }
}

@Composable
private fun BottomActions(
    state: LobbyUiState,
    lobby: Lobby,
    actions: LobbyActions,
) {
    val colors = CloakTheme.colors
    val me = state.myUid?.let(lobby::player)
    Column(verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
        if (state.isHost) {
            val blocker = StartCheck.blockers(lobby).firstOrNull()
            if (blocker !=
                null
            ) {
                Text(stringResource(blocker.message()), style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
                CloakButton(
                    stringResource(R.string.lobby_shuffle),
                    onClick = actions.onShuffle,
                    style = CloakButtonStyle.Glass,
                    enabled = lobby.players.size >= 2 && !state.busy,
                    modifier = Modifier.weight(1f),
                )
                CloakButton(
                    stringResource(R.string.lobby_start_briefing),
                    onClick = actions.onStartBriefing,
                    icon = CloakIcons.Play,
                    enabled = blocker == null && !state.busy,
                    modifier = Modifier.weight(1.3f),
                )
            }
        } else if (me != null) {
            Text(stringResource(R.string.lobby_waiting_host), style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
            CloakButton(
                stringResource(if (me.ready) R.string.lobby_not_ready_action else R.string.lobby_im_ready),
                onClick = { actions.onReady(!me.ready) },
                style = if (me.ready) CloakButtonStyle.Glass else CloakButtonStyle.Primary,
                icon = CloakIcons.Check,
                large = true,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun StartBlocker.message(): Int = when (this) {
    StartBlocker.NotEnoughPlayers -> R.string.blocker_not_enough
    StartBlocker.RolesUnassigned -> R.string.blocker_roles
    StartBlocker.NoSeeker -> R.string.blocker_no_seeker
    StartBlocker.NoHider -> R.string.blocker_no_hider
    StartBlocker.NotEveryoneReady -> R.string.blocker_not_ready
}

// The ellipse clears the code orb (about 206 x 120 dp) at every angle.
private val ORBIT_HEIGHT = 272.dp
private val ORBIT_RADIUS = 142.dp
private val AVATAR_SIZE = 46.dp
private const val ORBIT_TILT = 0.78f
private const val ORBIT_PERIOD_MS = 48_000
