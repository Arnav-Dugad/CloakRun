package com.arnavgames.cloakrun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnavgames.cloakrun.feature.update.UpdateBanner
import com.arnavgames.cloakrun.feature.update.UpdateSheetHost
import com.arnavgames.cloakrun.feature.update.UpdateViewModel
import com.arnavgames.cloakrun.feature.update.UpdaterConfig
import com.arnavgames.cloakrun.feature.update.UpdatesCard
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The sideload flavour updates itself from GitHub Releases. */
internal object AppUpdates : AppUpdatesUi {
    @Composable
    override fun LaunchCheck() {
        val viewModel: UpdateViewModel = hiltViewModel()
        LaunchedEffect(viewModel) { viewModel.check(force = false) }
    }

    @Composable
    override fun HomeBanner(onOpen: () -> Unit, modifier: Modifier) {
        val viewModel: UpdateViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        UpdateBanner(state, onOpen, modifier)
    }

    @Composable
    override fun AboutCard(onOpen: () -> Unit, modifier: Modifier) {
        UpdatesCard(hiltViewModel(), onOpen, modifier)
    }

    @Composable
    override fun SheetHost(visible: Boolean, onDismiss: () -> Unit) {
        UpdateSheetHost(hiltViewModel(), visible, onDismiss)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object UpdaterModule {
    @Provides
    fun config(): UpdaterConfig = UpdaterConfig(
        currentVersion = BuildConfig.VERSION_NAME,
        currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
        // Debug builds use a different package (.debug) and key, so they can never install a release.
        enabled = !BuildConfig.DEBUG,
    )
}
