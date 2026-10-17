package com.arnavgames.cloakrun

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.component.StatusPill
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme

/**
 * Always-visible, app-wide indicator that location is being shared. Shown whenever the match
 * tracking service runs, on every screen, so sharing is never invisible.
 */
@Composable
fun LocationSharingIndicator(sharing: Boolean, modifier: Modifier = Modifier) {
    val motion = CloakTheme.motion
    val description = stringResource(R.string.sharing_indicator_cd)
    AnimatedVisibility(
        visible = sharing,
        enter = fadeIn(motion.snappy()) + scaleIn(motion.bouncy(), initialScale = 0.8f),
        exit = fadeOut(motion.snappy()) + scaleOut(motion.snappy(), targetScale = 0.8f),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 4.dp)
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        StatusPill(
            roleLabel = stringResource(R.string.sharing_indicator),
            roleColor = CloakTheme.colors.accent,
            clock = "",
            sharingLocation = true,
        )
    }
}
