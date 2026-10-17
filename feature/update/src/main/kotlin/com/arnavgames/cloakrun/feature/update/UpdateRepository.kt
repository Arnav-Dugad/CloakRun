package com.arnavgames.cloakrun.feature.update

import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.updaterStore: DataStore<Preferences> by preferencesDataStore(name = "updater")

/**
 * Checks GitHub Releases for a newer CloakRun, downloads it with [DownloadManager] (progress
 * reported live), verifies it with [ApkVerifier], then installs through [PackageInstallerBridge].
 */
@Singleton
class UpdateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val config: UpdaterConfig,
    private val source: ReleaseSource,
    private val installer: PackageInstallerBridge,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val downloads = context.getSystemService<DownloadManager>()
    private val store = context.updaterStore
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private var work: Job? = null

    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val autoDownload: Flow<Boolean> = store.data.map { it[KEY_AUTO] ?: true }

    val isEnabled: Boolean get() = config.enabled

    init {
        installer.onResult = { release, success, cancelled ->
            _state.value = when {
                success -> UpdateState.UpToDate(release.version)
                cancelled -> UpdateState.ReadyToInstall(release)
                else -> UpdateState.Failed(UpdateFailure.Install, release)
            }
        }
        installer.onProgress = { release, f -> _state.value = UpdateState.Installing(release, f) }
        scope.launch { resumePendingDownload() }
    }

    suspend fun setAutoDownload(enabled: Boolean) {
        store.edit { it[KEY_AUTO] = enabled }
    }

    /** Checks for an update. Without [force], checks at most once per [CHECK_INTERVAL_MS]. */
    fun check(force: Boolean = false) {
        if (!config.enabled || work?.isActive == true) return
        work = scope.launch {
            val prefs = store.data.first()
            if (!force && System.currentTimeMillis() - (prefs[KEY_LAST_CHECK] ?: 0L) < CHECK_INTERVAL_MS) return@launch
            _state.value = UpdateState.Checking
            val release = runCatching { source.latest() }.getOrElse {
                _state.value = UpdateState.Failed(UpdateFailure.Network, null)
                return@launch
            }
            store.edit { it[KEY_LAST_CHECK] = System.currentTimeMillis() }
            val current = SemVer.parse(config.currentVersion) ?: SemVer(0, 0, 0)
            if (release == null || release.version <= current) {
                _state.value = UpdateState.UpToDate(current)
                return@launch
            }
            _state.value = UpdateState.Available(release)
            val unmetered = context.getSystemService<ConnectivityManager>()?.isActiveNetworkMetered == false
            if ((prefs[KEY_AUTO] ?: true) && unmetered) download(release)
        }
    }

    /** Starts (or restarts) downloading [release]. */
    fun download(release: ReleaseInfo) {
        val dm = downloads ?: run {
            _state.value = UpdateState.Failed(UpdateFailure.Download, release)
            return
        }
        scope.launch {
            val target = apkFile(release)
            withContext(Dispatchers.IO) { target.parentFile?.listFiles()?.forEach { it.delete() } }
            val request = DownloadManager.Request(release.apkUrl.toUri())
                .setTitle(context.getString(R.string.update_download_title, release.version.toString()))
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, DIRECTORY, target.name)
                .addRequestHeader("User-Agent", source.userAgent)
            val id = dm.enqueue(request)
            store.edit {
                it[KEY_DOWNLOAD_ID] = id
                it[KEY_DOWNLOAD_TAG] = release.tag
            }
            track(dm, id, release)
        }
    }

    /** Installs a verified download. */
    fun install(release: ReleaseInfo) {
        if (!installer.canInstall()) {
            _state.value = UpdateState.NeedsInstallPermission(release)
            return
        }
        _state.value = UpdateState.Installing(release, 0f)
        scope.launch { installer.install(apkFile(release), release) }
    }

    /** Continues an install after the user returns from the "install unknown apps" setting. */
    fun onReturnedFromPermissionSettings() {
        val s = _state.value
        if (s is UpdateState.NeedsInstallPermission && installer.canInstall()) install(s.release)
    }

    fun dismiss() {
        if (_state.value is UpdateState.Failed || _state.value is UpdateState.UpToDate) _state.value = UpdateState.Idle
    }

    private suspend fun track(
        dm: DownloadManager,
        id: Long,
        release: ReleaseInfo,
    ) {
        _state.value = UpdateState.Downloading(release, 0, release.apkSizeBytes)
        while (scope.isActive) {
            val snapshot = withContext(Dispatchers.IO) { DownloadSnapshot.of(dm, id) }
            when (snapshot?.status) {
                DownloadManager.STATUS_SUCCESSFUL -> return finishDownload(release, ok = true)

                null, DownloadManager.STATUS_FAILED -> return finishDownload(release, ok = false)

                else -> _state.value = UpdateState.Downloading(
                    release,
                    snapshot.downloaded,
                    if (snapshot.total > 0) snapshot.total else release.apkSizeBytes,
                )
            }
            delay(PROGRESS_POLL_MS)
        }
    }

    private suspend fun finishDownload(release: ReleaseInfo, ok: Boolean) {
        store.edit {
            it.remove(KEY_DOWNLOAD_ID)
            it.remove(KEY_DOWNLOAD_TAG)
        }
        if (!ok) {
            _state.value = UpdateState.Failed(UpdateFailure.Download, release)
            return
        }
        _state.value = UpdateState.Verifying(release)
        val file = apkFile(release)
        val failure = withContext(Dispatchers.IO) {
            val actual = Checksums.sha256(file)
            when {
                !ApkVerifier.hashMatches(actual, source.publishedSha256(release), release.apkServerSha256) -> UpdateFailure.Checksum
                !ApkVerifier.sameSigner(context, file) -> UpdateFailure.Signature
                else -> null
            }
        }
        if (failure != null) {
            withContext(Dispatchers.IO) { file.delete() }
            _state.value = UpdateState.Failed(failure, release)
        } else {
            _state.value = UpdateState.ReadyToInstall(release)
        }
    }

    private suspend fun resumePendingDownload() {
        val dm = downloads ?: return
        if (!config.enabled) return
        val prefs = store.data.first()
        val id = prefs[KEY_DOWNLOAD_ID] ?: return
        val release = runCatching { source.latest() }.getOrNull()
        if (release != null && release.tag == prefs[KEY_DOWNLOAD_TAG]) {
            track(dm, id, release)
        } else {
            store.edit { it.remove(KEY_DOWNLOAD_ID) }
        }
    }

    private fun apkFile(release: ReleaseInfo): File =
        File(context.getExternalFilesDir(DIRECTORY) ?: File(context.cacheDir, DIRECTORY), release.apkName).also { it.parentFile?.mkdirs() }

    private companion object {
        val KEY_AUTO = booleanPreferencesKey("auto_download")
        val KEY_LAST_CHECK = longPreferencesKey("last_check")
        val KEY_DOWNLOAD_ID = longPreferencesKey("download_id")
        val KEY_DOWNLOAD_TAG = stringPreferencesKey("download_tag")
        const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        const val PROGRESS_POLL_MS = 250L
        const val DIRECTORY = "updates"
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}

private class DownloadSnapshot(
    val status: Int,
    val downloaded: Long,
    val total: Long,
) {
    companion object {
        fun of(dm: DownloadManager, id: Long): DownloadSnapshot? = dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (!c.moveToFirst()) return@use null
            DownloadSnapshot(
                status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
            )
        }
    }
}
