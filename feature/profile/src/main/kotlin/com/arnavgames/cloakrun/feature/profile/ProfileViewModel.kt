package com.arnavgames.cloakrun.feature.profile

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arnavgames.cloakrun.core.data.AccountDeletion
import com.arnavgames.cloakrun.core.data.AuthRepository
import com.arnavgames.cloakrun.core.data.AuthUser
import com.arnavgames.cloakrun.core.data.Backend
import com.arnavgames.cloakrun.core.data.GoogleLinkResult
import com.arnavgames.cloakrun.core.data.PlayerPrefs
import com.arnavgames.cloakrun.core.data.PlayerProfile
import com.arnavgames.cloakrun.core.data.ProfileRepository
import com.arnavgames.cloakrun.core.game.profile.DisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val avatarSeed: String = "",
    val user: AuthUser? = null,
    val online: Boolean = true,
    val diagnostics: Boolean = false,
    val nameInvalid: Boolean = false,
    val busy: Boolean = false,
    val message: Int? = null,
    val pendingSwitch: GoogleLinkResult.AlreadyInUse? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: PlayerPrefs,
    private val profiles: ProfileRepository,
    private val auth: AuthRepository,
    private val backend: Backend,
    private val deletion: AccountDeletion,
) : ViewModel() {
    val name = TextFieldState()
    private val local = MutableStateFlow(ProfileUiState(online = backend.available))
    private val deleted = Channel<Unit>(Channel.CONFLATED)

    /** Fires after the account is gone; the app returns to onboarding. */
    val accountDeleted = deleted.receiveAsFlow()

    val state: StateFlow<ProfileUiState> = combine(local, auth.user, prefs.state) { l, user, p ->
        l.copy(user = user, diagnostics = p.diagnostics)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), local.value)

    init {
        viewModelScope.launch {
            prefs.current().profile?.let { p ->
                name.setTextAndPlaceCursorAtEnd(p.name)
                local.update { it.copy(avatarSeed = p.avatarSeed) }
            }
        }
    }

    fun shuffle() = local.update { it.copy(avatarSeed = DisplayName.avatarSeed()) }

    fun save() {
        val cleaned = DisplayName.clean(name.text.toString())
        if (DisplayName.problem(cleaned) != null) {
            local.update { it.copy(nameInvalid = true) }
            return
        }
        local.update { it.copy(nameInvalid = false) }
        viewModelScope.launch {
            profiles.save(PlayerProfile(cleaned, local.value.avatarSeed))
            local.update { it.copy(message = R.string.profile_saved) }
        }
    }

    fun setDiagnostics(enabled: Boolean) = viewModelScope.launch {
        prefs.setDiagnostics(enabled)
        backend.applyDiagnostics(enabled)
    }

    fun linkGoogle(activityContext: Context) = busy(R.string.profile_link_failed) {
        when (val result = auth.linkGoogle(activityContext)) {
            is GoogleLinkResult.Linked -> {
                profiles.sync()
                say(R.string.profile_linked)
            }

            is GoogleLinkResult.AlreadyInUse -> local.update { it.copy(pendingSwitch = result) }

            GoogleLinkResult.Cancelled -> say(R.string.profile_link_cancelled)

            GoogleLinkResult.NoGoogleAccount -> say(R.string.profile_no_google)

            is GoogleLinkResult.Failed -> say(R.string.profile_link_failed)
        }
    }

    fun confirmSwitch() = busy(R.string.profile_link_failed) {
        val pending = local.value.pendingSwitch ?: return@busy
        local.update { it.copy(pendingSwitch = null) }
        auth.switchTo(pending)
        // Adopt the account's saved profile, or keep this phone's one and upload it.
        val restored = profiles.restoreFromAccount()
        if (restored == null) {
            profiles.sync()
        } else {
            name.setTextAndPlaceCursorAtEnd(restored.name)
            local.update { it.copy(avatarSeed = restored.avatarSeed) }
        }
        say(R.string.profile_switched)
    }

    fun cancelSwitch() = local.update { it.copy(pendingSwitch = null) }

    fun deleteAccount(activityContext: Context) = busy(R.string.profile_delete_failed) {
        if (deletion.deleteEverything(activityContext)) deleted.send(Unit)
    }

    fun dismissMessage() = local.update { it.copy(message = null) }

    private fun say(message: Int) = local.update { it.copy(message = message) }

    private fun busy(failure: Int, block: suspend () -> Unit) {
        if (local.value.busy) return
        local.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                say(failure)
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
