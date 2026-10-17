package com.arnavgames.cloakrun.feature.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arnavgames.cloakrun.core.data.AuthRepository
import com.arnavgames.cloakrun.core.data.LobbyException
import com.arnavgames.cloakrun.core.data.LobbyHostRepository
import com.arnavgames.cloakrun.core.data.LobbyRepository
import com.arnavgames.cloakrun.core.data.LobbyUpdate
import com.arnavgames.cloakrun.core.data.PlayerPrefs
import com.arnavgames.cloakrun.core.game.lobby.Lobby
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.game.lobby.MatchPhase
import com.arnavgames.cloakrun.core.game.lobby.Role
import com.arnavgames.cloakrun.core.game.lobby.RoleAssignment
import com.arnavgames.cloakrun.core.game.lobby.StartCheck
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LobbyUiState(
    val update: LobbyUpdate? = null,
    val myUid: String? = null,
    val busy: Boolean = false,
    val message: Int? = null,
) {
    val lobby: Lobby? get() = (update as? LobbyUpdate.Live)?.lobby
    val online: Set<String> get() = (update as? LobbyUpdate.Live)?.online.orEmpty()
    val isHost: Boolean get() = myUid != null && lobby?.hostUid == myUid
}

/** One-off moments the screen turns into haptics or a notice. */
sealed interface LobbyEvent {
    data class Joined(val name: String) : LobbyEvent
}

@HiltViewModel(assistedFactory = LobbyViewModel.Factory::class)
class LobbyViewModel @AssistedInject constructor(
    @Assisted private val matchId: String,
    private val lobbies: LobbyRepository,
    private val hostRepo: LobbyHostRepository,
    auth: AuthRepository,
    private val prefs: PlayerPrefs,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(matchId: String): LobbyViewModel
    }

    private val myUid = auth.currentUid
    private val local = MutableStateFlow(LobbyUiState(myUid = myUid))
    private val events = Channel<LobbyEvent>(Channel.BUFFERED)
    val event = events.receiveAsFlow()

    // Seekers from the previous round rest next time, when possible.
    private var lastSeekers: Set<String> = emptySet()
    private var knownPlayers: Set<String>? = null

    private val updates = lobbies.observe(matchId).onEach { update ->
        when (update) {
            is LobbyUpdate.Live -> announceArrivals(update.lobby)
            else -> prefs.setActiveMatch(null)
        }
    }

    val state: StateFlow<LobbyUiState> = combine(updates, local) { update, l -> l.copy(update = update) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LobbyUiState(myUid = myUid))

    fun dismissMessage() = local.update { it.copy(message = null) }

    fun setReady(ready: Boolean) = act { lobbies.setReady(matchId, ready) }

    fun confirmBriefing() = act { lobbies.confirmBriefing(matchId) }

    fun leave(then: () -> Unit) = act {
        lobbies.leave(matchId)
        then()
    }

    /** Host-only actions. The Security Rules reject them from anyone else anyway. */
    val host = Host()

    inner class Host {
        fun shuffleRoles() = withLobby { lobby ->
            val roles = RoleAssignment.assign(lobby.players.map { it.uid }, lobby.config.seekers, lastSeekers)
            hostRepo.assignRoles(matchId, roles)
        }

        /** Tapping a player's role chip flips them between hider and seeker. */
        fun cycleRole(uid: String) = withLobby { lobby ->
            val current = lobby.player(uid)?.role ?: return@withLobby
            hostRepo.assignRoles(matchId, mapOf(uid to if (current == Role.Seeker) Role.Hider else Role.Seeker))
        }

        fun kick(uid: String) = act { hostRepo.kick(matchId, uid) }

        fun updateConfig(config: MatchConfig) = act { hostRepo.updateConfig(matchId, config) }

        fun startBriefing() = withLobby { lobby ->
            if (!StartCheck.canStart(lobby)) return@withLobby
            lastSeekers = lobby.players.filter { it.role == Role.Seeker }.map { it.uid }.toSet()
            hostRepo.setPhase(matchId, MatchPhase.Briefing)
        }

        fun backToLobby() = withLobby { lobby -> hostRepo.backToLobby(matchId, lobby.players.map { it.uid }) }

        fun end(then: () -> Unit) = act {
            hostRepo.end(matchId)
            then()
        }
    }

    private fun announceArrivals(lobby: Lobby) {
        val now = lobby.players.map { it.uid }.toSet()
        val before = knownPlayers
        knownPlayers = now
        if (before == null) return
        lobby.players.filter { it.uid !in before && it.uid != myUid }.forEach { events.trySend(LobbyEvent.Joined(it.name)) }
    }

    private fun withLobby(block: suspend (Lobby) -> Unit) = act { state.value.lobby?.let { block(it) } }

    private fun act(block: suspend () -> Unit) {
        if (local.value.busy) return
        local.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: LobbyException) {
                local.update { it.copy(message = e.failure.message()) }
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
