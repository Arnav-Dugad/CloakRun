package com.arnavgames.cloakrun.feature.playarea

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arnavgames.cloakrun.core.game.area.PlayAreaCheck
import com.arnavgames.cloakrun.core.game.area.PlayAreaRules
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.game.location.TrackingMode
import com.arnavgames.cloakrun.core.location.LocationRepository
import com.arnavgames.cloakrun.core.map.MapStylePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PlayAreaUiState(
    val draft: PlayAreaDraft = PlayAreaDraft(),
    val check: PlayAreaCheck = PlayAreaCheck.TooSmall,
    val fix: LocationFix? = null,
    val locationError: Boolean = false,
    val stylePreference: MapStylePreference = MapStylePreference.Auto,
    val rules: PlayAreaRules = PlayAreaRules(),
)

sealed interface PlayAreaIntent {
    data class SelectShape(val shape: AreaShape) : PlayAreaIntent
    data class MapTapped(val point: GeoPoint) : PlayAreaIntent
    data class RadiusChanged(val meters: Double) : PlayAreaIntent
    data class VertexMoved(val index: Int, val to: GeoPoint) : PlayAreaIntent
    data object Undo : PlayAreaIntent
    data object Clear : PlayAreaIntent
    data object Save : PlayAreaIntent
    data object CycleStyle : PlayAreaIntent
}

sealed interface PlayAreaEffect {
    data object Saved : PlayAreaEffect
}

@HiltViewModel
class PlayAreaViewModel @Inject constructor(private val location: LocationRepository, private val store: PlayAreaStore) : ViewModel() {
    private val rules = PlayAreaRules()
    private val draft = MutableStateFlow(PlayAreaDraft())
    private val style = MutableStateFlow(MapStylePreference.Auto)
    private val effects = Channel<PlayAreaEffect>(Channel.BUFFERED)
    private var trackingJob: Job? = null

    val effect = effects.receiveAsFlow()

    val state: StateFlow<PlayAreaUiState> = combine(draft, location.lastFix, location.error, style) { d, fix, error, s ->
        PlayAreaUiState(
            draft = d,
            check = d.check(rules),
            fix = fix,
            locationError = error != null && fix == null,
            stylePreference = s,
            rules = rules,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlayAreaUiState())

    init {
        viewModelScope.launch {
            store.saved.first()?.let { saved -> draft.value = PlayAreaDraft.from(saved) }
        }
    }

    /** Called while the screen is visible: location is only read then. */
    fun onVisible() {
        location.request(TRACKING_KEY, TrackingMode.Scouting)
        if (trackingJob?.isActive != true) {
            trackingJob = viewModelScope.launch {
                location.fixes.collect { fix ->
                    // Place a starting circle on the first fix if nothing was drawn yet.
                    draft.update { d -> if (d.circleCenter == null && d.vertices.isEmpty()) d.copy(circleCenter = fix.point) else d }
                }
            }
        }
    }

    fun onHidden() {
        trackingJob?.cancel()
        location.release(TRACKING_KEY)
    }

    fun onIntent(intent: PlayAreaIntent) {
        when (intent) {
            is PlayAreaIntent.SelectShape -> draft.update { it.withShape(intent.shape) }
            is PlayAreaIntent.MapTapped -> draft.update { it.tap(intent.point, rules) }
            is PlayAreaIntent.RadiusChanged -> draft.update { it.withRadius(intent.meters, rules) }
            is PlayAreaIntent.VertexMoved -> draft.update { it.moveVertex(intent.index, intent.to) }
            PlayAreaIntent.Undo -> draft.update { it.undo() }
            PlayAreaIntent.Clear -> draft.update { it.clear() }
            PlayAreaIntent.CycleStyle -> style.update { MapStylePreference.entries[(it.ordinal + 1) % MapStylePreference.entries.size] }
            PlayAreaIntent.Save -> save()
        }
    }

    private fun save() {
        val area = draft.value.area ?: return
        if (!rules.validate(area).isValid) return
        viewModelScope.launch {
            store.save(area)
            effects.send(PlayAreaEffect.Saved)
        }
    }

    override fun onCleared() {
        location.release(TRACKING_KEY)
    }

    private companion object {
        const val TRACKING_KEY = "play-area-editor"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
