package com.arnavgames.cloakrun.feature.lobby

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.data.LobbyUpdate
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.NightBackdrop
import com.arnavgames.cloakrun.core.designsystem.component.RadarLoader
import com.arnavgames.cloakrun.core.designsystem.component.StateMessage
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.haptics.HapticCue
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.lobby.InviteLink
import com.arnavgames.cloakrun.core.game.lobby.Lobby
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.game.lobby.MatchPhase
import com.arnavgames.cloakrun.core.icons.CloakIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Everything the lobby screens can ask for. */
class LobbyActions(
    val onReady: (Boolean) -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onCycleRole: (String) -> Unit = {},
    val onKick: (String) -> Unit = {},
    val onConfig: (MatchConfig) -> Unit = {},
    val onStartBriefing: () -> Unit = {},
    val onConfirmBriefing: () -> Unit = {},
    val onBackToLobby: () -> Unit = {},
    val onLeave: () -> Unit = {},
    val onEnd: () -> Unit = {},
    val onShare: (Lobby) -> Unit = {},
    val onCopy: (Lobby) -> Unit = {},
    val onHome: () -> Unit = {},
)

@Composable
fun LobbyRoute(
    matchId: String,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LobbyViewModel = hiltViewModel<LobbyViewModel, LobbyViewModel.Factory>(key = matchId) { it.create(matchId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val haptics = CloakTheme.haptics
    var notice by remember { mutableStateOf<String?>(null) }
    val copied = stringResource(R.string.lobby_copied)
    val joinedFormat = stringResource(R.string.lobby_joined)
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is LobbyEvent.Joined -> {
                    haptics.play(HapticCue.Ping)
                    notice = String.format(joinedFormat, event.name)
                }
            }
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MS)
            notice = null
        }
    }
    LaunchedEffect(state.message) { if (state.message != null) haptics.play(HapticCue.Error) }
    LobbyScreen(
        state = state,
        notice = notice,
        actions = LobbyActions(
            onReady = viewModel::setReady,
            onShuffle = viewModel.host::shuffleRoles,
            onCycleRole = viewModel.host::cycleRole,
            onKick = viewModel.host::kick,
            onConfig = viewModel.host::updateConfig,
            onStartBriefing = viewModel.host::startBriefing,
            onConfirmBriefing = viewModel::confirmBriefing,
            onBackToLobby = viewModel.host::backToLobby,
            onLeave = { viewModel.leave(onHome) },
            onEnd = { viewModel.host.end(onHome) },
            onShare = { shareInvite(context, it) },
            onCopy = {
                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("CloakRun code", it.code))) }
                notice = copied
            },
            onHome = onHome,
        ),
        modifier = modifier,
    )
}

private const val NOTICE_MS = 2_500L

private fun shareInvite(context: Context, lobby: Lobby) {
    val link = InviteLink.forCode(lobby.code)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.lobby_share_text, link, lobby.code))
    context.startActivity(Intent.createChooser(send, null))
}

@Composable
fun LobbyScreen(
    state: LobbyUiState,
    notice: String?,
    actions: LobbyActions,
    modifier: Modifier = Modifier,
) {
    val spacing = CloakTheme.spacing
    val dialogs = remember { LobbyDialogs() }
    val lobby = state.lobby
    // Back from a live lobby asks first when you are the host (it would end the lobby for everyone).
    BackHandler(enabled = lobby != null) { if (state.isHost) dialogs.confirmEnd = true else actions.onLeave() }
    GlassScene(backdrop = { NightBackdrop(focus = Offset(0.5f, 0.3f)) }, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = spacing.gutter, vertical = spacing.m)
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            val key = when {
                lobby == null -> state.update?.let { it::class.simpleName } ?: "loading"
                lobby.phase == MatchPhase.Lobby -> "lobby"
                else -> "briefing"
            }
            AnimatedContent(targetState = key, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "lobbyPhase") { k ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    when {
                        k == "lobby" && lobby != null -> LobbyRoom(state, lobby, actions, dialogs)
                        k == "briefing" && lobby != null -> Briefing(state, lobby, actions, onEndRequest = { dialogs.confirmEnd = true })
                        else -> Ending(state.update, actions.onHome)
                    }
                }
            }
        }
        notice?.let { Notice(it, Modifier.align(Alignment.TopCenter)) }
        state.message?.let { Notice(stringResource(it), Modifier.align(Alignment.TopCenter), isError = true) }
        if (lobby != null) {
            InviteModal(lobby, visible = dialogs.invite, onDismiss = { dialogs.invite = false })
            SettingsModal(lobby.config, visible = dialogs.settings, onDismiss = { dialogs.settings = false }, onChange = actions.onConfig)
        }
        KickModal(dialogs.kick, onDismiss = { dialogs.kick = null }, onConfirm = { uid ->
            dialogs.kick = null
            actions.onKick(uid)
        })
        EndLobbyModal(visible = dialogs.confirmEnd, onDismiss = { dialogs.confirmEnd = false }, onConfirm = actions.onEnd)
    }
}

@Composable
private fun Ending(update: LobbyUpdate?, onHome: () -> Unit) {
    if (update == null) {
        Box(Modifier.fillMaxWidth().padding(top = 120.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.l)) {
                RadarLoader(size = 140.dp)
                Text(stringResource(R.string.lobby_loading), style = CloakTheme.typography.body, color = CloakTheme.colors.textSecondary)
            }
        }
        return
    }
    val (title, body) = when (update) {
        LobbyUpdate.Kicked -> R.string.lobby_kicked_title to R.string.lobby_kicked_body
        LobbyUpdate.Left -> R.string.lobby_left_title to null
        else -> R.string.lobby_ended_title to R.string.lobby_ended_body
    }
    StateMessage(
        icon = if (update == LobbyUpdate.Kicked) CloakIcons.Lock else CloakIcons.Lobby,
        title = stringResource(title),
        body = body?.let { stringResource(it) }.orEmpty(),
        action = { CloakButton(stringResource(R.string.lobby_home), onClick = onHome, icon = CloakIcons.Back) },
    )
}
