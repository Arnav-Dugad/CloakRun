package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.arnavgames.cloakrun.core.designsystem.R
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme

/**
 * The CloakRun wordmark from the brand lockup. Its luminous gradient is made for dark grounds, so
 * on light themes it renders as the single-colour ink variant, never as a re-drawn logo.
 */
@Composable
fun CloakWordmark(contentDescription: String?, modifier: Modifier = Modifier) {
    val colors = CloakTheme.colors
    Image(
        painter = painterResource(R.drawable.cr_wordmark),
        contentDescription = contentDescription,
        colorFilter = if (colors.isDark) null else ColorFilter.tint(colors.textPrimary, BlendMode.SrcIn),
        modifier = modifier,
    )
}
