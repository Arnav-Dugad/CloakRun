package com.arnavgames.cloakrun.feature.update

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.core.designsystem.component.CloakButton
import com.arnavgames.cloakrun.core.designsystem.component.CloakButtonStyle
import com.arnavgames.cloakrun.core.designsystem.component.CloakToggle
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.component.NumberTicker
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons
import java.util.Locale

/**
 * A compact banner for the home screen. Hidden unless an update is available or in progress.
 * Tapping it opens [UpdateSheetHost]'s sheet through [onOpen].
 */
@Composable
fun UpdateBanner(
    state: UpdateState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val release = state.release() ?: return
    if (state is UpdateState.UpToDate || state is UpdateState.Idle || state is UpdateState.Checking) return
    val colors = CloakTheme.colors
    val title = when (state) {
        is UpdateState.Downloading, is UpdateState.Verifying ->
            stringResource(R.string.update_banner_downloading, release.version.toString())

        is UpdateState.ReadyToInstall, is UpdateState.Installing -> stringResource(R.string.update_banner_ready, release.version.toString())

        else -> stringResource(R.string.update_banner_available, release.version.toString())
    }
    GlassSurface(
        shape = SquircleShape(CloakTheme.radii.m),
        style = GlassDefaults.chrome(),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CloakIcon(CloakIcons.Star, contentDescription = null, tint = colors.accent, size = 20.dp)
                Text(title, style = CloakTheme.typography.bodyStrong, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(
                    stringResource(if (state is UpdateState.ReadyToInstall) R.string.update_restart else R.string.update_view),
                    style = CloakTheme.typography.label,
                    color = colors.accent,
                )
            }
            if (state is UpdateState.Downloading) LinearProgress(state.fraction ?: 0f)
        }
    }
}

/** Hosts the full-screen update sheet when [visible]. Place it at the root, above everything. */
@Composable
fun UpdateSheetHost(
    viewModel: UpdateViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val motion = CloakTheme.motion
    LifecycleResumeEffect(viewModel) {
        viewModel.onReturnedFromSettings()
        onPauseOrDispose { }
    }
    val release = state.release()
    AnimatedVisibility(
        visible = visible && release != null,
        enter = fadeIn(motion.smooth()),
        exit = fadeOut(motion.smooth()),
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier
                .fillMaxSize()
                .background(CloakTheme.colors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(motion.smooth()) { it / 2 },
                exit = slideOutVertically(motion.smooth()) { it / 2 },
            ) {
                if (release != null) {
                    UpdateSheet(
                        state = state,
                        release = release,
                        onUpdate = { viewModel.download(release) },
                        onInstall = { viewModel.install(release) },
                        onAllow = {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
internal fun UpdateSheet(
    state: UpdateState,
    release: ReleaseInfo,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CloakTheme.colors
    val notes = remember(release.notes) { ReleaseNotes.parse(release.notes) }
    GlassSurface(
        shape = SquircleShape(CloakTheme.radii.sheet),
        style = GlassDefaults.sheet(),
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(8.dp)
            .navigationBarsPadding()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(R.string.update_sheet_title, release.version.toString()),
                style = CloakTheme.typography.headline,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.update_sheet_subtitle, release.publishedAt.take(10), formatBytes(release.apkSizeBytes)),
                style = CloakTheme.typography.caption,
                color = colors.textTertiary,
            )
            Text(stringResource(R.string.update_whats_new), style = CloakTheme.typography.overline, color = colors.accent)
            Column(
                Modifier
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                notes.forEach { NoteRow(it) }
            }
            ProgressSection(state)
            Actions(state, onUpdate, onInstall, onAllow, onDismiss)
        }
    }
}

@Composable
private fun NoteRow(block: NoteBlock) {
    val colors = CloakTheme.colors
    when (block) {
        is NoteBlock.Heading -> Text(block.text, style = CloakTheme.typography.titleSmall, color = colors.textPrimary)

        is NoteBlock.Paragraph -> Text(block.text, style = CloakTheme.typography.bodySmall, color = colors.textSecondary)

        is NoteBlock.Bullet -> Row(Modifier.padding(start = (block.depth * 14).dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", style = CloakTheme.typography.bodySmall, color = colors.accent)
            Text(block.text, style = CloakTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ProgressSection(state: UpdateState) {
    val colors = CloakTheme.colors
    val (fraction, stage) = when (state) {
        is UpdateState.Downloading -> (state.fraction ?: 0f) to stringResource(R.string.update_stage_downloading)
        is UpdateState.Verifying -> 1f to stringResource(R.string.update_stage_verifying)
        is UpdateState.ReadyToInstall -> 1f to stringResource(R.string.update_stage_ready)
        is UpdateState.Installing -> state.fraction to stringResource(R.string.update_stage_installing)
        is UpdateState.NeedsInstallPermission -> 1f to stringResource(R.string.update_permission_body)
        is UpdateState.Failed -> 0f to stringResource(state.reason.message())
        else -> return
    }
    val animated by animateFloatAsState(fraction, CloakTheme.motion.smooth(), label = "updateProgress")
    val percent = (animated * 100).toInt()
    val description = stringResource(R.string.update_progress_cd, percent)
    val tint = if (state is UpdateState.Failed) colors.danger else colors.accent
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
            progressBarRangeInfo = ProgressBarRangeInfo(animated, 0f..1f)
            liveRegion = LiveRegionMode.Polite
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(64.dp)) {
                val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(tint.copy(alpha = 0.18f), style = stroke)
                drawArc(tint, -90f, 360f * animated, useCenter = false, style = stroke)
            }
            NumberTicker(
                "$percent",
                style = CloakTheme.typography.numeric.copy(fontSize = CloakTheme.typography.label.fontSize),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stage,
                style = CloakTheme.typography.bodyStrong,
                color = if (state is UpdateState.Failed) colors.danger else colors.textPrimary,
            )
            if (state is UpdateState.Downloading) {
                Text(
                    stringResource(R.string.update_progress_bytes, formatBytes(state.downloadedBytes), formatBytes(state.totalBytes)),
                    style = CloakTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
                    color = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun Actions(
    state: UpdateState,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CloakButton(
            stringResource(R.string.update_later),
            onClick = onDismiss,
            style = CloakButtonStyle.Ghost,
            modifier = Modifier.weight(1f),
        )
        val primary: Pair<Int, () -> Unit>? = when (state) {
            is UpdateState.Available -> R.string.update_now to onUpdate
            is UpdateState.ReadyToInstall -> R.string.update_install to onInstall
            is UpdateState.NeedsInstallPermission -> R.string.update_allow to onAllow
            is UpdateState.Failed -> R.string.update_retry to (if (state.reason == UpdateFailure.Install) onInstall else onUpdate)
            else -> null
        }
        if (primary != null) {
            CloakButton(stringResource(primary.first), onClick = primary.second, icon = CloakIcons.Check, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LinearProgress(fraction: Float) {
    val colors = CloakTheme.colors
    val animated by animateFloatAsState(fraction, CloakTheme.motion.smooth(), label = "bannerProgress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(colors.glassTintStrong, SquircleShape.Pill),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .background(colors.accent, SquircleShape.Pill),
        )
    }
}

/** About-screen card: current version, check now, and the auto-download preference. */
@Composable
fun UpdatesCard(
    viewModel: UpdateViewModel,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val auto by viewModel.autoDownload.collectAsStateWithLifecycle()
    val colors = CloakTheme.colors
    var asked by remember { mutableStateOf(false) }
    GlassCard(modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.update_card_title),
            style = CloakTheme.typography.titleSmall,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.update_card_current, viewModel.config.currentVersion),
            style = CloakTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        if (!viewModel.enabled) {
            Text(stringResource(R.string.update_card_disabled), style = CloakTheme.typography.caption, color = colors.textTertiary)
            return@GlassCard
        }
        val status = when (state) {
            UpdateState.Checking -> stringResource(R.string.update_card_checking)
            is UpdateState.UpToDate -> stringResource(R.string.update_card_up_to_date)
            is UpdateState.Failed -> stringResource((state as UpdateState.Failed).reason.message())
            else -> null
        }
        if (status != null && asked) Text(status, style = CloakTheme.typography.caption, color = colors.textTertiary)
        UpdateBanner(state, onOpen = onOpenSheet)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.update_card_auto),
                style = CloakTheme.typography.bodySmall,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            CloakToggle(auto, viewModel::setAutoDownload, contentDescription = stringResource(R.string.update_card_auto))
        }
        CloakButton(
            stringResource(R.string.update_card_check),
            onClick = {
                asked = true
                viewModel.check(force = true)
            },
            style = CloakButtonStyle.Glass,
            icon = CloakIcons.Replay,
            enabled = state !is UpdateState.Checking && state !is UpdateState.Downloading,
        )
    }
}

internal fun UpdateState.release(): ReleaseInfo? = when (this) {
    is UpdateState.Available -> release
    is UpdateState.Downloading -> release
    is UpdateState.Verifying -> release
    is UpdateState.ReadyToInstall -> release
    is UpdateState.NeedsInstallPermission -> release
    is UpdateState.Installing -> release
    is UpdateState.Failed -> release
    else -> null
}

private fun UpdateFailure.message(): Int = when (this) {
    UpdateFailure.Network -> R.string.update_failed_network
    UpdateFailure.Download -> R.string.update_failed_download
    UpdateFailure.Checksum -> R.string.update_failed_checksum
    UpdateFailure.Signature -> R.string.update_failed_signature
    UpdateFailure.Install -> R.string.update_failed_install
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format(Locale.ROOT, "%.0f kB", bytes / 1_000.0)
    else -> "$bytes B"
}
