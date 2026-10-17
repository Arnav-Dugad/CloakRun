package com.arnavgames.cloakrun.feature.update

/** Where the updater is. Exactly one state at a time; the UI renders it directly. */
sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class UpToDate(val current: SemVer) : UpdateState

    data class Available(val release: ReleaseInfo) : UpdateState

    /** Downloading the APK. [fraction] is null until the size is known. */
    data class Downloading(
        val release: ReleaseInfo,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdateState {
        val fraction: Float? get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else null
    }

    data class Verifying(val release: ReleaseInfo) : UpdateState

    /** Downloaded and verified; one tap installs. */
    data class ReadyToInstall(val release: ReleaseInfo) : UpdateState

    /** Android needs the user to allow CloakRun to install apps before we can continue. */
    data class NeedsInstallPermission(val release: ReleaseInfo) : UpdateState

    data class Installing(val release: ReleaseInfo, val fraction: Float) : UpdateState

    data class Failed(val reason: UpdateFailure, val release: ReleaseInfo?) : UpdateState
}

enum class UpdateFailure {
    /** No connection, GitHub unreachable, or rate limited. */
    Network,

    /** The download did not complete. */
    Download,

    /** The file's SHA-256 does not match the published checksum. Never installed. */
    Checksum,

    /** The APK is not signed by the same key as this app. Never installed. */
    Signature,

    /** Android rejected or cancelled the install. */
    Install,
}

/** How the updater behaves in this build, provided by the app's sideload source set. */
data class UpdaterConfig(
    val currentVersion: String,
    val currentVersionCode: Long,
    /** False in debug builds, whose package differs from the published release. */
    val enabled: Boolean,
    val owner: String = "Arnav-Dugad",
    val repo: String = "CloakRun",
) {
    val latestReleaseUrl: String get() = "https://api.github.com/repos/$owner/$repo/releases/latest"
}
