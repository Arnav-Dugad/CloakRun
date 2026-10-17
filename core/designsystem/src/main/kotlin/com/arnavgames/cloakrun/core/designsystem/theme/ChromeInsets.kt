package com.arnavgames.cloakrun.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extra space at the top of every screen reserved for app-wide chrome, such as the location
 * sharing indicator. Screens add it to their top bars so nothing is ever drawn underneath it.
 */
val LocalTopChromeInset = staticCompositionLocalOf<Dp> { 0.dp }
