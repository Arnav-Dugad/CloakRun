package com.arnavgames.cloakrun.feature.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads release metadata and small text assets from GitHub. */
class ReleaseSource @Inject constructor(private val config: UpdaterConfig) {
    val userAgent: String get() = "CloakRun-Android/${config.currentVersion} (Arnav Games)"

    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) { ReleaseParser.parse(get(config.latestReleaseUrl)) }

    suspend fun publishedSha256(release: ReleaseInfo): String? = withContext(Dispatchers.IO) {
        release.checksumsUrl?.let { url -> runCatching { Checksums.find(get(url), release.apkName) }.getOrNull() }
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
    }
}
