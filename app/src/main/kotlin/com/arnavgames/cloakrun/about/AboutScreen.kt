package com.arnavgames.cloakrun.about

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.R
import com.arnavgames.cloakrun.core.designsystem.R as DesignR
import com.arnavgames.cloakrun.core.designsystem.component.GlassCard
import com.arnavgames.cloakrun.core.designsystem.component.NightBackdrop
import com.arnavgames.cloakrun.core.designsystem.glass.GlassScene
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference

/**
 * About CloakRun: brand, the privacy promise, what this build contains, licences and credits.
 * In v0.1.0 it is also the launch destination.
 */
@Composable
fun AboutScreen(versionName: String, modifier: Modifier = Modifier) {
    val spacing = CloakTheme.spacing
    GlassScene(backdrop = { NightBackdrop(focus = Offset(0.5f, 0.2f)) }, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.xl)
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            EmblemHeader()
            Text(
                text = stringResource(R.string.tagline),
                style = CloakTheme.typography.body,
                color = CloakTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            VersionPill(versionName)
            Spacer(Modifier.height(spacing.s))
            InfoCard(
                title = stringResource(R.string.about_promise_title),
                body = stringResource(R.string.about_promise_body),
            )
            InfoCard(
                title = stringResource(R.string.about_build_title),
                body = stringResource(R.string.about_build_body),
            )
            InfoCard(
                title = stringResource(R.string.about_notices_title),
                body = stringResource(R.string.about_notices_body),
            )
            Spacer(Modifier.height(spacing.l))
            Text(
                text = stringResource(R.string.made_by),
                style = CloakTheme.typography.overline,
                color = CloakTheme.colors.accent,
            )
            Text(
                text = stringResource(R.string.about_copyright),
                style = CloakTheme.typography.caption,
                color = CloakTheme.colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmblemHeader() {
    val motion = CloakTheme.motion
    val scale = remember { Animatable(if (motion.reduced) 1f else 0.82f) }
    val alpha = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, motion.smooth())
    }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, motion.bouncy())
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(DesignR.drawable.cr_emblem),
            contentDescription = stringResource(R.string.cd_emblem),
            modifier = Modifier
                .padding(top = CloakTheme.spacing.xxl)
                .size(132.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
        )
        Spacer(Modifier.height(CloakTheme.spacing.xl))
        Image(
            painter = painterResource(DesignR.drawable.cr_wordmark),
            contentDescription = stringResource(R.string.cd_wordmark),
            modifier = Modifier
                .width(248.dp)
                .semantics { heading() }
                .graphicsLayer { this.alpha = alpha.value },
        )
    }
}

@Composable
private fun VersionPill(versionName: String) {
    val colors = CloakTheme.colors
    Text(
        text = stringResource(R.string.version_label, versionName),
        style = CloakTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
        color = colors.textSecondary,
        modifier = Modifier
            .background(colors.glassTint, SquircleShape.Pill)
            .border(BorderStroke(1.dp, colors.outline), SquircleShape.Pill)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun InfoCard(title: String, body: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = CloakTheme.typography.titleSmall,
            color = CloakTheme.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = body, style = CloakTheme.typography.bodySmall, color = CloakTheme.colors.textSecondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0A2B, widthDp = 393, heightDp = 852)
@Composable
private fun AboutScreenPreview() {
    CloakTheme(preference = ThemePreference.Night) {
        AboutScreen(versionName = "0.1.0")
    }
}
