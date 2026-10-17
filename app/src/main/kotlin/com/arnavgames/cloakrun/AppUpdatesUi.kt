package com.arnavgames.cloakrun

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The in-app updater as seen by shared code. The sideload flavour implements it with
 * feature:update (GitHub Releases); the play flavour ships a no-op, because Google Play updates the
 * app itself and restricts the install-packages permission.
 */
interface AppUpdatesUi {
    /** Checks for updates when the app starts (throttled inside the updater). */
    @Composable
    fun LaunchCheck()

    @Composable
    fun HomeBanner(onOpen: () -> Unit, modifier: Modifier)

    @Composable
    fun AboutCard(onOpen: () -> Unit, modifier: Modifier)

    @Composable
    fun SheetHost(visible: Boolean, onDismiss: () -> Unit)
}
