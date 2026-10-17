package com.arnavgames.cloakrun.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme

/**
 * A bottom-anchored glass panel over a scrim, for short tasks (show a QR code, confirm an action).
 * Tapping the scrim or pressing back dismisses it.
 */
@Composable
fun GlassModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = CloakTheme.motion
    AnimatedVisibility(visible = visible, enter = fadeIn(motion.smooth()), exit = fadeOut(motion.smooth())) {
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
                enter = slideInVertically(motion.bouncy()) { it / 2 },
                exit = slideOutVertically(motion.smooth()) { it / 2 },
            ) {
                GlassSurface(
                    shape = SquircleShape(CloakTheme.radii.sheet),
                    style = GlassDefaults.surface(),
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(CloakTheme.spacing.m)
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        // Swallow taps so they do not reach the scrim.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    Column(
                        Modifier.padding(CloakTheme.spacing.xl),
                        verticalArrangement = Arrangement.spacedBy(CloakTheme.spacing.m),
                    ) {
                        Text(
                            title,
                            style = CloakTheme.typography.title,
                            color = CloakTheme.colors.textPrimary,
                            modifier = Modifier.semantics { heading() },
                        )
                        content()
                    }
                }
            }
        }
    }
}
