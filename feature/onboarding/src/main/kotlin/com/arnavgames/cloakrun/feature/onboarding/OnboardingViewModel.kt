package com.arnavgames.cloakrun.feature.onboarding

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arnavgames.cloakrun.core.data.AgeBand
import com.arnavgames.cloakrun.core.data.Backend
import com.arnavgames.cloakrun.core.data.PlayerPrefs
import com.arnavgames.cloakrun.core.data.PlayerProfile
import com.arnavgames.cloakrun.core.data.PlayerState
import com.arnavgames.cloakrun.core.data.ProfileRepository
import com.arnavgames.cloakrun.core.game.profile.DisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { Loading, Age, Rules, Consent, Profile, Blocked }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Loading,
    val teen: Boolean = false,
    val diagnostics: Boolean = false,
    val avatarSeed: String = "",
    val nameProblem: DisplayName.Problem? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PlayerPrefs,
    private val profiles: ProfileRepository,
    private val backend: Backend,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /** The name being typed. Starts as a generated name so nobody has to type to play. */
    val name = TextFieldState()

    private val done = Channel<Unit>(Channel.CONFLATED)
    val finished = done.receiveAsFlow()

    init {
        viewModelScope.launch {
            val current = prefs.current()
            val profile = current.profile
            name.setTextAndPlaceCursorAtEnd(profile?.name ?: DisplayName.generate())
            _state.update {
                it.copy(
                    step = firstStep(current),
                    teen = current.ageBand == AgeBand.Teen,
                    diagnostics = current.diagnostics,
                    avatarSeed = profile?.avatarSeed ?: DisplayName.avatarSeed(),
                )
            }
        }
    }

    fun chooseAge(band: AgeBand) = viewModelScope.launch {
        prefs.setAgeBand(band)
        _state.update {
            it.copy(teen = band == AgeBand.Teen, step = if (band == AgeBand.Under13) OnboardingStep.Blocked else OnboardingStep.Rules)
        }
    }

    fun acceptRules() = viewModelScope.launch {
        prefs.acceptRules()
        _state.update { it.copy(step = OnboardingStep.Consent) }
    }

    fun setDiagnostics(enabled: Boolean) = _state.update { it.copy(diagnostics = enabled) }

    fun acceptConsent() = viewModelScope.launch {
        val diagnostics = _state.value.diagnostics
        prefs.acceptConsent(diagnostics)
        backend.applyDiagnostics(diagnostics)
        _state.update { it.copy(step = OnboardingStep.Profile) }
    }

    fun shuffleAvatar() = _state.update { it.copy(avatarSeed = DisplayName.avatarSeed()) }

    fun suggestName() {
        name.setTextAndPlaceCursorAtEnd(DisplayName.generate())
        _state.update { it.copy(nameProblem = null) }
    }

    fun finish() {
        val cleaned = DisplayName.clean(name.text.toString())
        val problem = DisplayName.problem(cleaned)
        _state.update { it.copy(nameProblem = problem) }
        if (problem != null || _state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            profiles.save(PlayerProfile(cleaned, _state.value.avatarSeed))
            done.send(Unit)
        }
    }

    /** Back steps through the flow; the age gate itself is final. */
    fun back(): Boolean {
        val previous = when (_state.value.step) {
            OnboardingStep.Rules -> OnboardingStep.Age
            OnboardingStep.Consent -> OnboardingStep.Rules
            OnboardingStep.Profile -> OnboardingStep.Consent
            else -> return false
        }
        // Going back to the age gate would allow retrying it, so the first step is the rules.
        if (previous == OnboardingStep.Age) return false
        _state.update { it.copy(step = previous) }
        return true
    }

    private fun firstStep(s: PlayerState): OnboardingStep = when {
        s.ageBand == AgeBand.Under13 -> OnboardingStep.Blocked
        s.ageBand == null -> OnboardingStep.Age
        !s.rulesAccepted -> OnboardingStep.Rules
        s.consentVersion < PlayerState.CURRENT_CONSENT -> OnboardingStep.Consent
        else -> OnboardingStep.Profile
    }
}
