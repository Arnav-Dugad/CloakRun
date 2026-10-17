package com.arnavgames.cloakrun.feature.playarea

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.CloakSlider
import com.arnavgames.cloakrun.core.designsystem.component.NumberTicker
import com.arnavgames.cloakrun.core.designsystem.component.SegmentedTabs
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.haptics.HapticCue
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.LocalTopChromeInset
import com.arnavgames.cloakrun.core.game.area.AreaUnit
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.area.PlayAreaCheck
import com.arnavgames.cloakrun.core.game.area.formatArea
import com.arnavgames.cloakrun.core.game.safety.HazardKind
import com.arnavgames.cloakrun.core.game.safety.SnapResult
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons
import com.arnavgames.cloakrun.core.location.hasLocationPermission
import com.arnavgames.cloakrun.core.map.CloakMap
import com.arnavgames.cloakrun.core.map.CloakMapState
import com.arnavgames.cloakrun.core.map.LocalMapOverlayContributors
import com.arnavgames.cloakrun.core.map.MapStyle
import com.arnavgames.cloakrun.core.map.MapStylePreference
import com.arnavgames.cloakrun.core.map.overlay.NoGoOverlay
import com.arnavgames.cloakrun.core.map.overlay.PlayAreaOverlay
import com.arnavgames.cloakrun.core.map.overlay.PlayerDotOverlay
import com.arnavgames.cloakrun.core.map.rememberCloakMapState
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayAreaRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayAreaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = CloakTheme.haptics
    var notice by remember { mutableStateOf<Int?>(null) }
    LifecycleStartEffect(viewModel) {
        viewModel.onVisible()
        onStopOrDispose { viewModel.onHidden() }
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            val message = when (effect) {
                PlayAreaEffect.Saved -> R.string.playarea_saved

                is PlayAreaEffect.Snap -> when (val r = effect.result) {
                    is SnapResult.Snapped -> if (r.cutAtHazards) R.string.playarea_snapped_cut else R.string.playarea_snapped
                    SnapResult.NotAPark -> R.string.playarea_not_park
                    SnapResult.TooSmall -> R.string.playarea_park_small
                }
            }
            val success = effect is PlayAreaEffect.Saved || (effect as? PlayAreaEffect.Snap)?.result is SnapResult.Snapped
            haptics.play(if (success) HapticCue.Confirm else HapticCue.Error)
            notice = message
            delay(SAVED_NOTICE_MS)
            notice = null
        }
    }
    PlayAreaScreen(state = state, notice = notice, onIntent = viewModel::onIntent, onBack = onBack, modifier = modifier)
}

@Composable
fun PlayAreaScreen(
    state: PlayAreaUiState,
    notice: Int?,
    onIntent: (PlayAreaIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapState = rememberCloakMapState()
    val motion = CloakTheme.motion
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = context.hasLocationPermission()
    }
    var flewIn by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(mapState.isReady, state.fix != null) {
        val fix = state.fix
        if (!flewIn && mapState.isReady && fix != null) {
            mapState.cinematicFlyIn(fix.point, motion.reduced)
            flewIn = true
        }
    }
    val area = state.draft.area
    val contributors = LocalMapOverlayContributors.current
    val viewport = LocalWindowInfo.current.containerSize
    val safety = rememberSafetyCheck(mapState, area, onIntent)
    var panelExpanded by rememberSaveable { mutableStateOf(true) }
    // After a park snap, get the panel out of the way so the new outline is visible.
    LaunchedEffect(state.draft.vertices) {
        if (state.draft.shape == AreaShape.Park && state.draft.vertices.isNotEmpty()) panelExpanded = false
    }
    val onMapTap: (com.arnavgames.cloakrun.core.game.geo.GeoPoint) -> Unit = { point ->
        val screen = mapState.toScreen(point)
        when {
            state.draft.shape != AreaShape.Park -> onIntent(PlayAreaIntent.MapTapped(point))

            screen != null -> {
                val w = viewport.width.toFloat()
                val h = viewport.height.toFloat()
                onIntent(PlayAreaIntent.ParkTapped(point, mapState.parkPiecesAt(screen, w, h), mapState.visibleHazards(w, h)))
            }
        }
    }

    GlassScene(
        modifier = modifier.fillMaxSize(),
        backdrop = {
            CloakMap(
                state = mapState,
                style = mapStyleFor(state),
                modifier = Modifier.fillMaxSize(),
                onMapClick = onMapTap,
            ) {
                NoGoOverlay(mapState, area, state.safety.hazards)
                PlayAreaOverlay(mapState, area, valid = state.canSave, editing = state.draft.shape.usesVertices)
                PlayerDotOverlay(mapState, state.fix)
            }
        },
    ) {
        if (state.draft.shape.usesVertices) {
            state.draft.vertices.forEachIndexed { index, _ -> VertexHandle(mapState, state, index, onIntent) }
        }
        contributors.forEach { it.Overlay(mapState, area) }
        TopBar(state.stylePreference, onBack = onBack, onCycleStyle = { onIntent(PlayAreaIntent.CycleStyle) })
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(CloakTheme.spacing.m)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s),
        ) {
            if (!hasPermission) {
                PermissionCard(
                    onAllow = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                )
            } else if (state.locationError) {
                Notice(stringResource(R.string.playarea_location_off))
            }
            notice?.let { Notice(stringResource(it)) }
            ToolPanel(
                state = state,
                onIntent = onIntent,
                onCenterOnMe = { state.fix?.let { mapState.animateTo(it.point, zoom = 16.0) } },
                safetyPartial = safety.partial,
                onCheckSafety = safety.check,
                expanded = panelExpanded,
                onToggle = { panelExpanded = !panelExpanded },
            )
        }
    }
}

private fun mapStyleFor(state: PlayAreaUiState): MapStyle = when (state.stylePreference) {
    MapStylePreference.Night -> MapStyle.Night
    MapStylePreference.Day -> MapStyle.Day
    MapStylePreference.Auto -> state.fix?.let { MapStyle.forTime(System.currentTimeMillis(), it.point) } ?: MapStyle.Night
}

/** Whether the last safety pass saw only part of the area, and an action that frames the area and re-runs it. */
private class SafetyCheck(val partial: Boolean, val check: () -> Unit)

@Composable
private fun rememberSafetyCheck(
    mapState: CloakMapState,
    area: PlayArea?,
    onIntent: (PlayAreaIntent) -> Unit,
): SafetyCheck {
    val motion = CloakTheme.motion
    val viewport = LocalWindowInfo.current.containerSize
    val scope = rememberCoroutineScope()
    var partial by remember { mutableStateOf(false) }
    val reportHazards: () -> Unit = {
        onIntent(PlayAreaIntent.HazardsSeen(mapState.visibleHazards(viewport.width.toFloat(), viewport.height.toFloat())))
    }
    // Re-run the safety pass when the area changes and the whole area is on screen.
    LaunchedEffect(area, mapState.isReady) {
        if (area == null || !mapState.isReady) return@LaunchedEffect
        delay(SAFETY_SETTLE_MS)
        partial = mapState.zoom < CloakMapState.SAFETY_MIN_ZOOM - 1 || !area.outline(48).all { p ->
            mapState.toScreen(p)?.let { it.x in 0f..viewport.width.toFloat() && it.y in 0f..viewport.height.toFloat() } == true
        }
        if (!partial) reportHazards()
    }
    return SafetyCheck(partial) {
        if (area != null) {
            scope.launch {
                mapState.fitAndAwaitIdle(area.outline(48), paddingPx = viewport.width / 8, reducedMotion = motion.reduced)
                partial = false
                reportHazards()
            }
        }
    }
}

@Composable
private fun TopBar(
    style: MapStylePreference,
    onBack: () -> Unit,
    onCycleStyle: () -> Unit,
) {
    val colors = CloakTheme.colors
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = LocalTopChromeInset.current)
            .fillMaxWidth()
            .padding(horizontal = CloakTheme.spacing.m, vertical = CloakTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s),
    ) {
        RoundGlassButton(CloakIcons.Back, stringResource(R.string.playarea_back), onBack)
        GlassSurface(shape = SquircleShape.Pill, style = GlassDefaults.chrome(), modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.playarea_title),
                style = CloakTheme.typography.titleSmall,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp).semantics { heading() },
            )
        }
        val styleLabel = when (style) {
            MapStylePreference.Auto -> R.string.playarea_style_auto
            MapStylePreference.Night -> R.string.playarea_style_night
            MapStylePreference.Day -> R.string.playarea_style_day
        }
        val styleIcon = when (style) {
            MapStylePreference.Auto -> CloakIcons.Compass
            MapStylePreference.Night -> CloakIcons.Moon
            MapStylePreference.Day -> CloakIcons.Sun
        }
        RoundGlassButton(styleIcon, stringResource(styleLabel), onCycleStyle)
    }
}

@Composable
private fun RoundGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val haptics = CloakTheme.haptics
    GlassSurface(
        shape = SquircleShape.Pill,
        style = GlassDefaults.chrome(),
        modifier = Modifier
            .size(CloakTheme.sizes.buttonHeight)
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable {
                haptics.play(HapticCue.Tick)
                onClick()
            },
    ) {
        CloakIcon(icon, contentDescription = null, tint = CloakTheme.colors.textPrimary, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ToolPanel(
    state: PlayAreaUiState,
    onIntent: (PlayAreaIntent) -> Unit,
    onCenterOnMe: () -> Unit,
    safetyPartial: Boolean,
    onCheckSafety: () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = CloakTheme.colors
    val draft = state.draft
    GlassSurface(shape = SquircleShape(CloakTheme.radii.sheet), style = GlassDefaults.sheet(), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CloakTheme.spacing.l), verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { AreaReadoutRow(state) }
                PanelToggle(expanded, onToggle)
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m)) {
                    ToolDetails(state, onIntent, onCenterOnMe, safetyPartial, onCheckSafety)
                }
            }
        }
    }
}

@Composable
private fun PanelToggle(expanded: Boolean, onToggle: () -> Unit) {
    val label = stringResource(if (expanded) R.string.playarea_collapse else R.string.playarea_expand)
    val rotation by animateFloatAsState(if (expanded) 90f else -90f, CloakTheme.motion.snappy(), label = "chevron")
    Box(
        Modifier
            .size(CloakTheme.sizes.minTouchTarget)
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        CloakIcon(
            CloakIcons.ChevronRight,
            contentDescription = null,
            tint = CloakTheme.colors.textSecondary,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}

@Composable
private fun ColumnScope.ToolDetails(
    state: PlayAreaUiState,
    onIntent: (PlayAreaIntent) -> Unit,
    onCenterOnMe: () -> Unit,
    safetyPartial: Boolean,
    onCheckSafety: () -> Unit,
) {
    val colors = CloakTheme.colors
    val draft = state.draft
    SegmentedTabs(
        options = persistentListOf(
            stringResource(R.string.playarea_shape_circle),
            stringResource(R.string.playarea_shape_polygon),
            stringResource(R.string.playarea_shape_park),
        ),
        selectedIndex = draft.shape.ordinal,
        onSelect = { onIntent(PlayAreaIntent.SelectShape(AreaShape.entries[it])) },
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.draft.area != null) SafetyRow(state, safetyPartial, onCheckSafety)
    Text(
        stringResource(
            when (draft.shape) {
                AreaShape.Circle -> R.string.playarea_hint_circle
                AreaShape.Polygon -> R.string.playarea_hint_polygon
                AreaShape.Park -> R.string.playarea_hint_park
            },
        ),
        style = CloakTheme.typography.bodySmall,
        color = colors.textSecondary,
    )
    if (draft.shape == AreaShape.Circle) {
        val rules = state.rules
        val fraction = ((draft.radiusM - rules.minCircleRadiusM) / (rules.maxCircleRadiusM - rules.minCircleRadiusM)).toFloat()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.playarea_radius),
                style = CloakTheme.typography.label,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.playarea_radius_value, draft.radiusM.roundToInt()),
                style = CloakTheme.typography.numeric.copy(fontSize = CloakTheme.typography.label.fontSize),
                color = colors.textPrimary,
            )
        }
        CloakSlider(
            value = fraction,
            onValueChange = { f ->
                onIntent(
                    PlayAreaIntent.RadiusChanged(rules.minCircleRadiusM + f * (rules.maxCircleRadiusM - rules.minCircleRadiusM)),
                )
            },
            contentDescription = stringResource(R.string.playarea_radius),
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
            CloakButton(
                stringResource(R.string.playarea_undo),
                onClick = { onIntent(PlayAreaIntent.Undo) },
                style = CloakButtonStyle.Glass,
                icon = CloakIcons.Replay,
                enabled = draft.vertices.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
            CloakButton(
                stringResource(R.string.playarea_clear),
                onClick = { onIntent(PlayAreaIntent.Clear) },
                style = CloakButtonStyle.Ghost,
                enabled = draft.vertices.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
        CloakButton(
            stringResource(R.string.playarea_center_on_me),
            onClick = onCenterOnMe,
            style = CloakButtonStyle.Glass,
            icon = CloakIcons.Location,
            enabled = state.fix != null,
            modifier = Modifier.weight(1f),
        )
        CloakButton(
            stringResource(R.string.playarea_save),
            onClick = { onIntent(PlayAreaIntent.Save) },
            icon = CloakIcons.Check,
            enabled = state.canSave,
            modifier = Modifier.weight(1f),
        )
    }
    Text(stringResource(R.string.playarea_safety), style = CloakTheme.typography.caption, color = colors.textTertiary)
}

@Composable
private fun AreaReadoutRow(state: PlayAreaUiState) {
    val colors = CloakTheme.colors
    val readout = formatArea(state.draft.area?.areaM2 ?: 0.0)
    val number = when (readout.unit) {
        AreaUnit.SquareMetres -> String.format(Locale.ROOT, "%,.0f", readout.value)
        AreaUnit.Hectares -> String.format(Locale.ROOT, "%.1f", readout.value)
        AreaUnit.SquareKilometres -> String.format(Locale.ROOT, "%.2f", readout.value)
    }
    val unit = stringResource(
        when (readout.unit) {
            AreaUnit.SquareMetres -> R.string.playarea_unit_m2
            AreaUnit.Hectares -> R.string.playarea_unit_ha
            AreaUnit.SquareKilometres -> R.string.playarea_unit_km2
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.playarea_area_label), style = CloakTheme.typography.overline, color = colors.textTertiary)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumberTicker(number, style = CloakTheme.typography.headline)
                Text(unit, style = CloakTheme.typography.label, color = colors.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        ValidityBadge(state)
    }
}

@Composable
private fun ValidityBadge(state: PlayAreaUiState) {
    val colors = CloakTheme.colors
    val rules = state.rules
    val valid = state.check.isValid
    val message = when (state.check) {
        PlayAreaCheck.Valid -> stringResource(R.string.playarea_valid)
        PlayAreaCheck.TooSmall -> stringResource(R.string.playarea_too_small, describeArea(rules.minAreaM2))
        PlayAreaCheck.TooLarge -> stringResource(R.string.playarea_too_large, describeArea(rules.maxAreaM2))
        PlayAreaCheck.SelfIntersecting -> stringResource(R.string.playarea_self_intersecting)
        PlayAreaCheck.TooFewVertices -> stringResource(R.string.playarea_too_few)
        PlayAreaCheck.TooManyVertices -> pluralStringResource(R.plurals.playarea_too_many, rules.maxVertices, rules.maxVertices)
    }
    val tint = if (valid) colors.success else colors.warning
    Row(
        modifier = Modifier.widthIn(max = 190.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CloakIcon(if (valid) CloakIcons.Check else CloakIcons.Warning, contentDescription = null, tint = tint, size = 18.dp)
        Text(message, style = CloakTheme.typography.caption, color = tint)
    }
}

@Composable
private fun describeArea(m2: Double): String {
    val r = formatArea(m2)
    val unit = stringResource(
        when (r.unit) {
            AreaUnit.SquareMetres -> R.string.playarea_unit_m2
            AreaUnit.Hectares -> R.string.playarea_unit_ha
            AreaUnit.SquareKilometres -> R.string.playarea_unit_km2
        },
    )
    return String.format(Locale.ROOT, "%.1f %s", r.value, unit)
}

@Composable
private fun PermissionCard(onAllow: () -> Unit) {
    GlassSurface(shape = SquircleShape(CloakTheme.radii.card), style = GlassDefaults.sheet(), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CloakTheme.spacing.l), verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.s)) {
            Text(
                stringResource(R.string.playarea_permission_title),
                style = CloakTheme.typography.titleSmall,
                color = CloakTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.playarea_permission_body),
                style = CloakTheme.typography.bodySmall,
                color = CloakTheme.colors.textSecondary,
            )
            CloakButton(stringResource(R.string.playarea_permission_allow), onClick = onAllow, icon = CloakIcons.Location)
        }
    }
}

@Composable
private fun Notice(text: String) {
    GlassSurface(shape = SquircleShape.Pill, style = GlassDefaults.chrome()) {
        Text(
            text,
            style = CloakTheme.typography.bodySmall,
            color = CloakTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

/** A draggable corner handle, positioned in the layout phase on every map frame. */
@Composable
private fun VertexHandle(
    mapState: CloakMapState,
    state: PlayAreaUiState,
    index: Int,
    onIntent: (PlayAreaIntent) -> Unit,
) {
    val handle = 44.dp
    val label = stringResource(R.string.playarea_corner_handle, index + 1)
    val current by rememberUpdatedState(state)
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var dragTotal by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .offset {
                @Suppress("UNUSED_EXPRESSION")
                mapState.frame
                val vertex = current.draft.vertices.getOrNull(index)
                // Zoomed far out the handles would only clutter, so they step off screen.
                val visible = mapState.zoom >= HANDLE_MIN_ZOOM
                val p = vertex?.takeIf { visible }?.let(mapState::toScreen) ?: Offset(-1_000f, -1_000f)
                val half = handle.roundToPx() / 2
                IntOffset(p.x.roundToInt() - half, p.y.roundToInt() - half)
            }
            .size(handle)
            .semantics { contentDescription = label }
            .pointerInput(index) {
                detectDragGestures(
                    onDragStart = {
                        dragStart = current.draft.vertices.getOrNull(index)?.let(mapState::toScreen) ?: Offset.Zero
                        dragTotal = Offset.Zero
                    },
                ) { change, amount ->
                    change.consume()
                    dragTotal += amount
                    mapState.toGeo(dragStart + dragTotal)?.let { onIntent(PlayAreaIntent.VertexMoved(index, it)) }
                }
            },
    )
}

private const val SAVED_NOTICE_MS = 2_400L
private const val SAFETY_SETTLE_MS = 700L
private const val HANDLE_MIN_ZOOM = 14.0

@Composable
private fun SafetyRow(
    state: PlayAreaUiState,
    partial: Boolean,
    onCheck: () -> Unit,
) {
    val colors = CloakTheme.colors
    val safety = state.safety
    val counts = listOfNotNull(
        safety.countByKind[HazardKind.MajorRoad]?.let { pluralStringResource(R.plurals.playarea_hazard_roads, it, it) },
        safety.countByKind[HazardKind.Rail]?.let { pluralStringResource(R.plurals.playarea_hazard_rail, it, it) },
        safety.countByKind[HazardKind.Water]?.let { pluralStringResource(R.plurals.playarea_hazard_water, it, it) },
        safety.countByKind[HazardKind.Restricted]?.let { pluralStringResource(R.plurals.playarea_hazard_restricted, it, it) },
        safety.countByKind[HazardKind.Building]?.let { pluralStringResource(R.plurals.playarea_hazard_buildings, it, it) },
    )
    val message = when {
        safety.isMostlyUnsafe -> stringResource(R.string.playarea_safety_unsafe)
        counts.isEmpty() -> stringResource(R.string.playarea_safety_none)
        else -> stringResource(R.string.playarea_safety_counts, counts.joinToString(", "), (safety.coveredFraction * 100).toInt())
    }
    val tint = if (safety.isMostlyUnsafe) colors.danger else colors.accent
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CloakIcon(CloakIcons.Shield, contentDescription = null, tint = tint, size = 18.dp)
            Text(stringResource(R.string.playarea_safety_title), style = CloakTheme.typography.label, color = colors.textPrimary)
        }
        if (state.safetyChecked) {
            Text(
                message,
                style = CloakTheme.typography.bodySmall,
                color = if (safety.isMostlyUnsafe) colors.danger else colors.textSecondary,
            )
            if (safety.hazards.isNotEmpty()) {
                Text(stringResource(R.string.playarea_safety_hint), style = CloakTheme.typography.caption, color = colors.textTertiary)
            }
        }
        if (partial) {
            Text(stringResource(R.string.playarea_safety_partial), style = CloakTheme.typography.caption, color = colors.warning)
            CloakButton(
                stringResource(R.string.playarea_safety_check),
                onClick = onCheck,
                style = CloakButtonStyle.Glass,
                icon = CloakIcons.Shield,
            )
        }
    }
}
