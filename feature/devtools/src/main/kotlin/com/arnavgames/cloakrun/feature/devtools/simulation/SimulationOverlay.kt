package com.arnavgames.cloakrun.feature.devtools.simulation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.CloakToggle
import com.arnavgames.cloakrun.core.designsystem.component.SegmentedTabs
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.LocalTopChromeInset
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.integrity.IntegrityReport
import com.arnavgames.cloakrun.core.game.integrity.RoutePlausibility
import com.arnavgames.cloakrun.core.game.location.FixSource
import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.game.safety.Hazard
import com.arnavgames.cloakrun.core.game.safety.HazardKind
import com.arnavgames.cloakrun.core.location.LocationRepository
import com.arnavgames.cloakrun.core.location.TrackingController
import com.arnavgames.cloakrun.core.map.CloakMapState
import com.arnavgames.cloakrun.core.map.MapOverlayContributor
import com.arnavgames.cloakrun.feature.devtools.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Debug-only map layer: simulated bots, the Simulation panel and the debug HUD. */
class SimulationOverlay(private val repository: LocationRepository, private val tracking: TrackingController) : MapOverlayContributor {
    // Process-wide so a simulation keeps running when you navigate between map screens.
    private val engine by lazy { SimulationEngine(repository) }

    @Composable
    override fun Overlay(state: CloakMapState, playArea: PlayArea?) {
        val fps = rememberFps()
        val integrity = rememberIntegrity(state)
        LaunchedEffect(engine) {
            var last = 0L
            while (isActive) {
                withFrameNanos { now ->
                    if (last != 0L) engine.tick((now - last) / 1e9)
                    last = now
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            BotsLayer(state, engine.bots)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 76.dp + LocalTopChromeInset.current, end = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DebugHud(fps, engine.bots.size, integrity)
                SimulationPanel(
                    engine = engine,
                    onSpawn = { engine.spawn(state.visibleWalkableLines(widthPx, heightPx), playArea) },
                )
            }
        }
    }

    @Composable
    private fun DebugHud(
        fps: Int,
        bots: Int,
        integrity: IntegrityReport,
    ) {
        val fix by repository.lastFix.collectAsStateWithLifecycle()
        val fixText = fix?.let { "%.0fm".format(it.accuracyM) } ?: stringResource(R.string.sim_hud_no_fix)
        val source = when {
            fix == null -> ""
            fix?.isMock == true -> stringResource(R.string.sim_hud_mock)
            fix?.source == FixSource.Simulated -> FixSource.Simulated.name.lowercase()
            else -> stringResource(R.string.sim_hud_real)
        }
        GlassSurface(shape = SquircleShape.Pill, style = GlassDefaults.chrome(), elevated = false) {
            Text(
                stringResource(R.string.sim_hud, fps, fixText, source, bots) + "   " +
                    stringResource(R.string.sim_integrity, integrity.score, integrity.verdict.name),
                style = CloakTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
                color = CloakTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }

    @Composable
    private fun SimulationPanel(engine: SimulationEngine, onSpawn: () -> Boolean) {
        var open by remember { mutableStateOf(false) }
        var noStreets by remember { mutableStateOf(false) }
        val colors = CloakTheme.colors
        GlassSurface(shape = SquircleShape(CloakTheme.radii.m), style = GlassDefaults.chrome()) {
            Column(Modifier.width(if (open) 260.dp else 140.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.clickable { open = !open },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sim_toggle),
                        style = CloakTheme.typography.label,
                        color = colors.accent,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (open) {
                    CloakButton(
                        stringResource(R.string.sim_spawn),
                        onClick = { noStreets = !onSpawn() },
                        style = CloakButtonStyle.Glass,
                    )
                    if (noStreets) {
                        Text(stringResource(R.string.sim_no_streets), style = CloakTheme.typography.caption, color = colors.warning)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.sim_walk_me),
                            style = CloakTheme.typography.bodySmall,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        CloakToggle(engine.walkingMe, engine::setWalkMe, contentDescription = stringResource(R.string.sim_walk_me))
                    }
                    val speeds = listOf(1.0, 4.0, 10.0)
                    SegmentedTabs(
                        options = persistentListOf(
                            stringResource(R.string.sim_speed_1),
                            stringResource(R.string.sim_speed_4),
                            stringResource(R.string.sim_speed_10),
                        ),
                        selectedIndex = speeds.indexOf(engine.speed).coerceAtLeast(0),
                        onSelect = { engine.changeSpeed(speeds[it]) },
                    )
                    val context = LocalContext.current
                    val serviceOn by tracking.isTracking.collectAsStateWithLifecycle()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.sim_tracking),
                            style = CloakTheme.typography.bodySmall,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        CloakToggle(
                            serviceOn,
                            { on -> if (on) tracking.start(context) else tracking.stop() },
                            contentDescription = stringResource(R.string.sim_tracking),
                        )
                    }
                    CloakButton(
                        stringResource(R.string.sim_spoof),
                        onClick = engine::teleportMe,
                        style = CloakButtonStyle.Danger,
                        enabled = engine.walkingMe,
                    )
                    CloakButton(stringResource(R.string.sim_clear), onClick = engine::clear, style = CloakButtonStyle.Ghost)
                }
            }
        }
    }

    /**
     * Live route plausibility for your own trail (last ten minutes), against the bots' street graph
     * and the buildings on screen. Recomputed every two seconds.
     */
    @Composable
    private fun rememberIntegrity(state: CloakMapState): IntegrityReport {
        var report by remember { mutableStateOf(IntegrityReport.Clean) }
        val windowInfo = LocalWindowInfo.current
        LaunchedEffect(engine) {
            val trail = ArrayDeque<LocationFix>()
            launch {
                repository.lastFix.collect { fix ->
                    if (fix == null || trail.lastOrNull()?.timeMillis == fix.timeMillis) return@collect
                    trail.addLast(fix)
                    while (trail.size > 1 && fix.timeMillis - trail.first().timeMillis > TRAIL_WINDOW_MS) trail.removeFirst()
                }
            }
            var buildings: List<List<GeoPoint>> = emptyList()
            var buildingsAt = 0L
            while (isActive) {
                delay(INTEGRITY_INTERVAL_MS)
                // Only judge a trail while the simulated "me" walks; the real device trail is judged in matches.
                val origin = trail.firstOrNull()?.point
                if (engine.walkingMe && origin != null) {
                    val now = System.currentTimeMillis()
                    if (now - buildingsAt > BUILDINGS_REFRESH_MS) {
                        val size = windowInfo.containerSize
                        buildings = state.visibleHazards(size.width.toFloat(), size.height.toFloat())
                            .filterIsInstance<Hazard.Area>()
                            .filter { it.kind == HazardKind.Building }
                            .map { it.ring }
                        buildingsAt = now
                    }
                    report = RoutePlausibility(engine.graph, buildings, origin).evaluate(trail.toList())
                }
            }
        }
        return report
    }
}

@Composable
private fun BotsLayer(state: CloakMapState, bots: List<SimBotView>) {
    val colors = CloakTheme.colors
    val measurer = rememberTextMeasurer()
    val label = CloakTheme.typography.caption.copy(color = colors.textPrimary)
    Canvas(Modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        state.frame
        for (bot in bots) {
            val p = state.toScreen(bot.position) ?: continue
            when (bot.team) {
                SimTeam.Seeker -> drawSeekerCone(p, (bot.headingDeg - state.bearing).toFloat(), colors.teamSeeker)
                SimTeam.Hider -> drawGlassBead(p, colors.teamHider)
            }
            drawNameTag(measurer, bot.name, p, label)
        }
    }
}

private fun DrawScope.drawSeekerCone(
    p: Offset,
    headingDeg: Float,
    color: Color,
) {
    val length = 46.dp.toPx()
    val half = 24.dp.toPx()
    rotate(headingDeg, pivot = p) {
        val cone = Path().apply {
            moveTo(p.x, p.y)
            lineTo(p.x - half, p.y - length)
            quadraticTo(p.x, p.y - length * 1.18f, p.x + half, p.y - length)
            close()
        }
        drawPath(
            cone,
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(color.copy(alpha = 0f), color.copy(alpha = 0.45f)),
                startY =
                    p.y - length,
                endY = p.y,
            ),
        )
    }
    drawCircle(Color.White, 7.dp.toPx(), p)
    drawCircle(color, 5.dp.toPx(), p)
}

private fun DrawScope.drawGlassBead(p: Offset, color: Color) {
    drawCircle(color.copy(alpha = 0.25f), 12.dp.toPx(), p)
    drawCircle(color, 7.dp.toPx(), p)
    drawCircle(Color.White.copy(alpha = 0.7f), 2.5.dp.toPx(), Offset(p.x - 2.dp.toPx(), p.y - 2.dp.toPx()))
}

private fun DrawScope.drawNameTag(
    measurer: TextMeasurer,
    name: String,
    p: Offset,
    style: androidx.compose.ui.text.TextStyle,
) {
    val layout = measurer.measure(name, style)
    drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y + 12.dp.toPx()))
}

/** Frames per second over the last second, measured from the Compose frame clock. */
@Composable
private fun rememberFps(): Int {
    var fps by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var frames = 0
        var windowStart = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (windowStart == 0L) windowStart = now
                frames++
                if (now - windowStart >= 1_000_000_000L) {
                    fps = frames
                    frames = 0
                    windowStart = now
                }
            }
        }
    }
    return fps
}

private const val TRAIL_WINDOW_MS = 10 * 60 * 1000L
private const val INTEGRITY_INTERVAL_MS = 2_000L
private const val BUILDINGS_REFRESH_MS = 30_000L
