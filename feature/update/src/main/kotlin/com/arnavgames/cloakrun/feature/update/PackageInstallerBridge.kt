package com.arnavgames.cloakrun.feature.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Installs a verified APK through a [PackageInstaller] session, streaming it in with progress.
 *
 * The first update shows the system confirmation. Once CloakRun has installed itself, Android 12+
 * lets later updates proceed without a prompt (USER_ACTION_NOT_REQUIRED); Android decides.
 */
@Singleton
class PackageInstallerBridge @Inject constructor(@param:ApplicationContext private val context: Context) {
    internal var onResult: (ReleaseInfo, success: Boolean, cancelled: Boolean) -> Unit = { _, _, _ -> }
    internal var onProgress: (ReleaseInfo, Float) -> Unit = { _, _ -> }
    private var pending: ReleaseInfo? = null

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    suspend fun install(apk: File, release: ReleaseInfo) {
        pending = release
        val ok = runCatching { writeAndCommit(apk, release) }.isSuccess
        if (!ok) onResult(release, false, false)
    }

    private suspend fun writeAndCommit(apk: File, release: ReleaseInfo) = withContext(Dispatchers.IO) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = packageInstaller.createSession(params)
        packageInstaller.openSession(sessionId).use { session ->
            val total = apk.length().coerceAtLeast(1)
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { input ->
                    val buffer = ByteArray(BUFFER)
                    var written = 0L
                    var lastReported = 0f
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        written += n
                        val fraction = written.toFloat() / total
                        session.setStagingProgress(fraction)
                        if (fraction - lastReported >= REPORT_STEP) {
                            lastReported = fraction
                            withContext(Dispatchers.Main) { onProgress(release, fraction) }
                        }
                    }
                }
                session.fsync(out)
            }
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallStatusReceiver::class.java).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(callback.intentSender)
        }
    }

    internal fun handleStatus(intent: Intent) {
        val release = pending ?: return
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm != null) {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    onResult(release, false, false)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> onResult(release, true, false)

            PackageInstaller.STATUS_FAILURE_ABORTED -> onResult(release, false, true)

            else -> onResult(release, false, false)
        }
    }

    private companion object {
        const val BUFFER = 64 * 1024
        const val REPORT_STEP = 0.02f
    }
}

/** Receives PackageInstaller session results and hands them to [PackageInstallerBridge]. */
@dagger.hilt.android.AndroidEntryPoint
class InstallStatusReceiver : BroadcastReceiver() {
    @Inject lateinit var bridge: PackageInstallerBridge

    override fun onReceive(context: Context, intent: Intent) {
        bridge.handleStatus(intent)
    }
}
