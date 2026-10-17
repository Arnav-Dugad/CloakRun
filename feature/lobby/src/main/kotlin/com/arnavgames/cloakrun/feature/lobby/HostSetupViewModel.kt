package com.arnavgames.cloakrun.feature.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arnavgames.cloakrun.core.data.LobbyException
import com.arnavgames.cloakrun.core.data.LobbyFailure
import com.arnavgames.cloakrun.core.data.LobbyRepository
import com.arnavgames.cloakrun.core.data.PlayAreaStore
import com.arnavgames.cloakrun.core.data.PlayerPrefs
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HostSetupUiState(
    val loading: Boolean = true,
    val area: PlayArea? = null,
    val config: MatchConfig = MatchConfig(),
    val creating: Boolean = false,
    val failure: LobbyFailure? = null,
)

@HiltViewModel
class HostSetupViewModel @Inject constructor(
    private val areas: PlayAreaStore,
    private val lobbies: LobbyRepository,
    private val prefs: PlayerPrefs,
) : ViewModel() {
    private val _state = MutableStateFlow(HostSetupUiState())
    val state: StateFlow<HostSetupUiState> = _state.asStateFlow()

    private val created = Channel<String>(Channel.CONFLATED)

    /** Emits the new match id once the lobby exists. */
    val lobbyCreated = created.receiveAsFlow()

    init {
        viewModelScope.launch {
            areas.saved.collect { area -> _state.update { it.copy(loading = false, area = area) } }
        }
    }

    fun setSeekers(n: Int) = edit { it.copy(seekers = n.coerceAtMost(it.maxPlayers - 1)) }

    fun setHeadStart(seconds: Int) = edit { it.copy(headStartSeconds = seconds) }

    fun setHunt(minutes: Int) = edit { it.copy(huntMinutes = minutes) }

    fun setMaxPlayers(n: Int) = edit { it.copy(maxPlayers = n, seekers = it.seekers.coerceAtMost(n - 1)) }

    fun create() {
        val s = _state.value
        val area = s.area ?: return
        if (s.creating) return
        _state.update { it.copy(creating = true, failure = null) }
        viewModelScope.launch {
            try {
                val profile = requireNotNull(prefs.current().profile)
                created.send(lobbies.create(area, s.config, profile))
            } catch (e: LobbyException) {
                _state.update { it.copy(failure = e.failure) }
            } finally {
                _state.update { it.copy(creating = false) }
            }
        }
    }

    private fun edit(change: (MatchConfig) -> MatchConfig) = _state.update { it.copy(config = change(it.config)) }
}
