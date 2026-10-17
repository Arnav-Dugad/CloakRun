package com.arnavgames.cloakrun

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Google Play keeps the app up to date, so the play flavour has no in-app updater. */
internal object AppUpdates : AppUpdatesUi {
    @Composable
    override fun LaunchCheck() = Unit

    @Composable
    override fun HomeBanner(onOpen: () -> Unit, modifier: Modifier) = Unit

    @Composable
    override fun AboutCard(onOpen: () -> Unit, modifier: Modifier) = Unit

    @Composable
    override fun SheetHost(visible: Boolean, onDismiss: () -> Unit) = Unit
}
