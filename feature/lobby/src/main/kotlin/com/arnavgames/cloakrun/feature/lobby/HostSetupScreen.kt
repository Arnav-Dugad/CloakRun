package com.arnavgames.cloakrun.feature.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.CloakChip
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.component.NightBackdrop
import com.arnavgames.cloakrun.core.designsystem.component.StateMessage
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons
import java.util.Locale

@Composable
fun HostSetupRoute(
    onBack: () -> Unit,
    onDesignArea: () -> Unit,
    onLobbyOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HostSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onLobbyOpen)
    LaunchedEffect(viewModel) { viewModel.lobbyCreated.collect { open(it) } }
    HostSetupScreen(
        state = state,
        onBack = onBack,
        onDesignArea = onDesignArea,
        onSeekers = viewModel::setSeekers,
        onHeadStart = viewModel::setHeadStart,
        onHunt = viewModel::setHunt,
        onMaxPlayers = viewModel::setMaxPlayers,
        onCreate = viewModel::create,
        modifier = modifier,
    )
}

@Composable
fun HostSetupScreen(
    state: HostSetupUiState,
    onBack: () -> Unit,
    onDesignArea: () -> Unit,
    onSeekers: (Int) -> Unit,
    onHeadStart: (Int) -> Unit,
    onHunt: (Int) -> Unit,
    onMaxPlayers: (Int) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CloakTheme.spacing
    val colors = CloakTheme.colors
    GlassScene(backdrop = { NightBackdrop(focus = Offset(0.8f, 0.1f)) }, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.m)
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            ScreenTitle(R.string.host_title, onBack)
            val area = state.area
            when {
                state.loading -> Unit

                area == null -> StateMessage(
                    icon = CloakIcons.Zone,
                    title = stringResource(R.string.host_no_area_title),
                    body = stringResource(R.string.host_no_area_body),
                    action = {
                        CloakButton(stringResource(R.string.host_no_area_action), onClick = onDesignArea, icon = CloakIcons.Map)
                    },
                )

                else -> {
                    AreaCard(area, onDesignArea)
                    SettingsCard(state.config, onSeekers, onHeadStart, onHunt, onMaxPlayers)
                    Text(stringResource(R.string.host_settings_note), style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
                    state.failure?.let {
                        Text(stringResource(it.message()), style = CloakTheme.typography.bodyStrong, color = colors.danger)
                    }
                    Spacer(Modifier.height(spacing.s))
                    CloakButton(
                        stringResource(R.string.host_create),
                        onClick = onCreate,
                        icon = CloakIcons.Lobby,
                        large = true,
                        enabled = !state.creating,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ScreenTitle(title: Int, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
        CloakButton(stringResource(R.string.lobby_back), onClick = onBack, style = CloakButtonStyle.Ghost, icon = CloakIcons.Back)
        Text(
            stringResource(title),
            style = CloakTheme.typography.headline,
            color = CloakTheme.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun AreaCard(area: PlayArea, onEdit: () -> Unit) {
    val colors = CloakTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m)) {
            CloakIcon(CloakIcons.Zone, contentDescription = null, tint = colors.accent, size = 32.dp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.host_area), style = CloakTheme.typography.overline, color = colors.textSecondary)
                Text(areaSize(area), style = CloakTheme.typography.title, color = colors.textPrimary)
                Text(
                    when (area) {
                        is PlayArea.Circle -> stringResource(R.string.host_area_circle, distance(area.radiusM * 2))
                        is PlayArea.Polygon -> stringResource(R.string.host_area_polygon, area.vertices.size)
                    },
                    style = CloakTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        CloakButton(stringResource(R.string.host_area_edit), onClick = onEdit, style = CloakButtonStyle.Glass, icon = CloakIcons.Map)
    }
}

@Composable
private fun SettingsCard(
    config: MatchConfig,
    onSeekers: (Int) -> Unit,
    onHeadStart: (Int) -> Unit,
    onHunt: (Int) -> Unit,
    onMaxPlayers: (Int) -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        ChoiceRow(R.string.host_max_players, listOf(4, 6, 8), config.maxPlayers, { stringResource(R.string.host_up_to, it) }, onMaxPlayers)
        ChoiceRow(R.string.host_seekers, (1..3).filter { it < config.maxPlayers }, config.seekers, { it.toString() }, onSeekers)
        ChoiceRow(
            R.string.host_head_start,
            listOf(60, 120, 300),
            config.headStartSeconds,
            { stringResource(R.string.host_minutes, it / 60) },
            onHeadStart,
        )
        ChoiceRow(R.string.host_hunt, listOf(10, 20, 30, 45), config.huntMinutes, { stringResource(R.string.host_minutes, it) }, onHunt)
    }
}

@Composable
internal fun ChoiceRow(
    title: Int,
    options: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(title), style = CloakTheme.typography.overline, color = CloakTheme.colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
            options.forEach { option -> CloakChip(label(option), selected = option == selected, onClick = { onSelect(option) }) }
        }
    }
}

internal fun areaSize(area: PlayArea): String {
    val m2 = area.areaM2
    return if (m2 >=
        10_000
    ) {
        String.format(Locale.getDefault(), "%.1f ha", m2 / 10_000)
    } else {
        String.format(Locale.getDefault(), "%.0f m²", m2)
    }
}

private fun distance(meters: Double): String = if (meters >=
    1000
) {
    String.format(Locale.getDefault(), "%.1f km", meters / 1000)
} else {
    String.format(Locale.getDefault(), "%.0f m", meters)
}
