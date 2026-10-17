package com.arnavgames.cloakrun.feature.update

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A semantic version (major.minor.patch). Build suffixes such as "-debug" are ignored. */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int = compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val pattern = Regex("""^v?(\d+)\.(\d+)\.(\d+)""")

        fun parse(text: String): SemVer? = pattern.find(text.trim())?.destructured?.let { (a, b, c) ->
            SemVer(a.toInt(), b.toInt(), c.toInt())
        }
    }
}

/** A published release that this build could update to. */
data class ReleaseInfo(
    val version: SemVer,
    val tag: String,
    val notes: String,
    val publishedAt: String,
    val apkName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    /** SHA-256 computed by GitHub when the asset was uploaded (hex), if provided. */
    val apkServerSha256: String?,
    val checksumsUrl: String?,
)

@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
internal data class GitHubAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url") val downloadUrl: String,
    val digest: String? = null,
)

object ReleaseParser {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses the GitHub "latest release" response. Returns null for drafts, pre-releases, or
     * releases without a CloakRun APK, so a malformed release can never be offered.
     */
    fun parse(body: String): ReleaseInfo? {
        val release = runCatching { json.decodeFromString(GitHubRelease.serializer(), body) }.getOrNull() ?: return null
        if (release.draft || release.prerelease) return null
        val version = SemVer.parse(release.tagName) ?: return null
        val apkName = "CloakRun-v$version.apk"
        val apk = release.assets.firstOrNull { it.name == apkName } ?: return null
        val sums = release.assets.firstOrNull { it.name == "CloakRun-v$version-SHA256SUMS.txt" }
        return ReleaseInfo(
            version = version,
            tag = release.tagName,
            notes = release.body.orEmpty(),
            publishedAt = release.publishedAt.orEmpty(),
            apkName = apk.name,
            apkUrl = apk.downloadUrl,
            apkSizeBytes = apk.size,
            apkServerSha256 = apk.digest?.removePrefix("sha256:")?.lowercase()?.takeIf { it.length == SHA256_HEX_LENGTH },
            checksumsUrl = sums?.downloadUrl,
        )
    }

    private const val SHA256_HEX_LENGTH = 64
}

object Checksums {
    /** Finds the hex SHA-256 for [fileName] in a `sha256sum` style listing ("<hex>  <name>"). */
    fun find(listing: String, fileName: String): String? = listing.lineSequence()
        .map { it.trim() }
        .mapNotNull { line ->
            val parts = line.split(Regex("""\s+"""), limit = 2)
            if (parts.size == 2 && parts[1].trimStart('*') == fileName) parts[0].lowercase() else null
        }
        .firstOrNull { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }

    fun sha256(file: File): String = file.inputStream().use(::sha256)

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

/** A block of release notes, rendered without a full Markdown engine. */
sealed interface NoteBlock {
    data class Heading(val text: String) : NoteBlock

    data class Bullet(val text: String, val depth: Int) : NoteBlock

    data class Paragraph(val text: String) : NoteBlock
}

object ReleaseNotes {
    /**
     * Turns the release body into readable blocks. Code blocks (the checksum listing) and the
     * commit list are dropped: players care about what changed, not about hashes or commit IDs.
     */
    fun parse(markdown: String): List<NoteBlock> {
        val blocks = mutableListOf<NoteBlock>()
        var inCode = false
        var skipSection = false
        for (raw in markdown.lines()) {
            val line = raw.trimEnd()
            if (line.trimStart().startsWith("```")) inCode = !inCode
            val heading = HEADING.find(line)
            when {
                inCode || line.isBlank() || line.trimStart().startsWith("```") -> Unit

                heading != null -> {
                    val title = clean(heading.groupValues[1])
                    skipSection = title.equals("Commits", ignoreCase = true) || title.startsWith("Verify", ignoreCase = true)
                    if (!skipSection) blocks += NoteBlock.Heading(title)
                }

                !skipSection -> addBodyLine(blocks, raw, line)
            }
        }
        return blocks
    }

    private fun addBodyLine(
        blocks: MutableList<NoteBlock>,
        raw: String,
        line: String,
    ) {
        val bullet = BULLET.find(line)
        when {
            bullet != null -> blocks += NoteBlock.Bullet(clean(bullet.groupValues[2]), bullet.groupValues[1].length / 2)

            continuesBullet(blocks.lastOrNull(), raw) -> {
                val last = blocks.removeAt(blocks.lastIndex) as NoteBlock.Bullet
                blocks += last.copy(text = last.text + " " + clean(line.trim()))
            }

            else -> blocks += NoteBlock.Paragraph(clean(line.trim()))
        }
    }

    private val HEADING = Regex("""^#{1,6}\s+(.*)$""")
    private val BULLET = Regex("""^(\s*)[-*]\s+(.*)$""")

    /** A wrapped line belongs to the bullet above only if it is indented deeper than that bullet's marker. */
    private fun continuesBullet(last: NoteBlock?, raw: String): Boolean {
        if (last !is NoteBlock.Bullet) return false
        val indent = raw.length - raw.trimStart().length
        return indent > last.depth * 2
    }

    private fun clean(text: String): String = text.replace("**", "").replace("`", "").trim()
}
