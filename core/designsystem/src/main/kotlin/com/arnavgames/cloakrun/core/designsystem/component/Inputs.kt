package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.glass.GlassDefaults
import com.arnavgames.cloakrun.core.designsystem.glass.GlassSurface
import com.arnavgames.cloakrun.core.designsystem.shape.SquircleShape
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.token.CloakMotion
import com.arnavgames.cloakrun.core.icons.CloakIcon
import com.arnavgames.cloakrun.core.icons.CloakIcons

/** A glass text field. The rim lights up in the primary colour while focused. */
@Composable
fun CloakTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTransformation: InputTransformation? = null,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    val colors = CloakTheme.colors
    val motion = CloakTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val rim by animateColorAsState(
        when {
            isError -> colors.danger
            focused -> colors.primary
            else -> Color.Transparent
        },
        motion.snappy(),
        label = "fieldRim",
    )
    val shape = SquircleShape(CloakTheme.radii.m)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = CloakTheme.typography.overline, color = colors.textSecondary)
        GlassSurface(shape = shape, style = GlassDefaults.chrome(), elevated = false, modifier = Modifier.border(1.5.dp, rim, shape)) {
            BasicTextField(
                state = state,
                interactionSource = interaction,
                textStyle = CloakTheme.typography.bodyStrong.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = keyboardOptions,
                inputTransformation = inputTransformation,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CloakTheme.sizes.buttonHeight)
                    .padding(horizontal = CloakTheme.spacing.l),
                decorator = { inner ->
                    Box(Modifier.heightIn(min = CloakTheme.sizes.buttonHeight), contentAlignment = Alignment.CenterStart) {
                        if (state.text.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = CloakTheme.typography.body, color = colors.textTertiary)
                        }
                        inner()
                    }
                },
            )
        }
        if (supportingText != null) {
            Text(
                supportingText,
                style = CloakTheme.typography.caption,
                color = if (isError) colors.danger else colors.textTertiary,
            )
        }
    }
}

/**
 * The in-match status pill: a compact glass capsule with the role dot (pulsing gently), the
 * role name and the match clock.
 */
@Composable
fun StatusPill(
    roleLabel: String,
    roleColor: Color,
    clock: String,
    modifier: Modifier = Modifier,
    sharingLocation: Boolean = true,
    sharingLabel: String = "",
) {
    val colors = CloakTheme.colors
    val reduced = CloakTheme.motion.reduced
    val pulse by if (reduced) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "pill").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(CloakMotion.BREATH_PERIOD_MS / 2, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse",
        )
    }
    GlassSurface(shape = SquircleShape.Pill, style = GlassDefaults.chrome(), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .drawBehind {
                        drawCircle(roleColor.copy(alpha = 0.35f * (1f - pulse)), radius = size.minDimension * (0.5f + pulse * 0.6f))
                        drawCircle(roleColor, radius = size.minDimension / 2)
                    },
            )
            Text(roleLabel, style = CloakTheme.typography.label, color = colors.textPrimary)
            NumberTicker(clock, style = CloakTheme.typography.numeric.copy(fontSize = CloakTheme.typography.label.fontSize))
            if (sharingLocation) {
                CloakIcon(CloakIcons.Location, contentDescription = sharingLabel.ifEmpty { null }, tint = colors.accent, size = 16.dp)
            }
        }
    }
}
