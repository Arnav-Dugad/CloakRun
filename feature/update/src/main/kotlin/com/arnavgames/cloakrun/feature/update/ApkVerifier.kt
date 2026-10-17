package com.arnavgames.cloakrun.feature.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/** Integrity checks a downloaded APK must pass before it may be installed. */
internal object ApkVerifier {
    /**
     * The hash must agree with every source that is available (published SHA256SUMS and GitHub's
     * asset digest), and at least one source must exist.
     */
    fun hashMatches(
        actual: String,
        published: String?,
        server: String?,
    ): Boolean = (published != null || server != null) && (published == null || published == actual) && (server == null || server == actual)

    /** The APK must be this app and be signed only by certificates the installed app trusts. */
    fun sameSigner(context: Context, file: File): Boolean {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(file.absolutePath, signingFlags()) ?: return false
        if (archive.packageName != context.packageName) return false
        val mine = signerDigests(pm.getPackageInfo(context.packageName, signingFlags()))
        val theirs = signerDigests(archive)
        return mine.isNotEmpty() && theirs.isNotEmpty() && theirs.all { it in mine }
    }

    private fun signingFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val certs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { s -> if (s.hasMultipleSigners()) s.apkContentsSigners else s.signingCertificateHistory }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return certs.orEmpty().map { sig ->
            MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }
}
