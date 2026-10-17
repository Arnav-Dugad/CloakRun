package com.arnavgames.cloakrun.feature.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UpdateViewModel @Inject constructor(private val repository: UpdateRepository, val config: UpdaterConfig) : ViewModel() {
    val state: StateFlow<UpdateState> = repository.state

    val autoDownload: StateFlow<Boolean> = repository.autoDownload.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val enabled: Boolean get() = repository.isEnabled

    fun check(force: Boolean) = repository.check(force)

    fun download(release: ReleaseInfo) = repository.download(release)

    fun install(release: ReleaseInfo) = repository.install(release)

    fun onReturnedFromSettings() = repository.onReturnedFromPermissionSettings()

    fun dismiss() = repository.dismiss()

    fun setAutoDownload(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoDownload(enabled) }
    }
}
