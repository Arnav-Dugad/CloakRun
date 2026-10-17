package com.arnavgames.cloakrun.feature.lobby

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arnavgames.cloakrun.core.data.LobbyException
import com.arnavgames.cloakrun.core.data.LobbyRepository
import com.arnavgames.cloakrun.core.data.PlayerPrefs
import com.arnavgames.cloakrun.core.game.lobby.InviteLink
import com.arnavgames.cloakrun.core.game.lobby.JoinCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JoinUiState(
    val joining: Boolean = false,
    val message: Int? = null,
    val complete: Boolean = false,
)

@HiltViewModel
class JoinViewModel @Inject constructor(private val lobbies: LobbyRepository, private val prefs: PlayerPrefs) : ViewModel() {
    val code = TextFieldState()

    private val _state = MutableStateFlow(JoinUiState())
    val state: StateFlow<JoinUiState> = _state.asStateFlow()

    private val joined = Channel<String>(Channel.CONFLATED)

    /** Emits the match id once joined. */
    val lobbyJoined = joined.receiveAsFlow()

    init {
        viewModelScope.launch {
            snapshotFlow { code.text.toString() }.collect { text ->
                _state.update { it.copy(complete = JoinCode.isValid(text), message = null) }
            }
        }
    }

    /** From an invite link or a QR code: fill the code in and join straight away. */
    fun joinFromInvite(text: String) {
        val parsed = InviteLink.parse(text)
        if (parsed == null) {
            _state.update { it.copy(message = R.string.join_bad_qr) }
            return
        }
        code.setTextAndPlaceCursorAtEnd(parsed)
        join()
    }

    fun scannerUnavailable() = _state.update { it.copy(message = R.string.join_scanner_unavailable) }

    fun join() {
        val joinCode = JoinCode.normalize(code.text.toString()) ?: return
        if (_state.value.joining) return
        _state.update { it.copy(joining = true, message = null) }
        viewModelScope.launch {
            try {
                val profile = requireNotNull(prefs.current().profile)
                val matchId = lobbies.find(joinCode)
                lobbies.join(matchId, profile)
                joined.send(matchId)
            } catch (e: LobbyException) {
                _state.update { it.copy(message = e.failure.message()) }
            } finally {
                _state.update { it.copy(joining = false) }
            }
        }
    }
}
