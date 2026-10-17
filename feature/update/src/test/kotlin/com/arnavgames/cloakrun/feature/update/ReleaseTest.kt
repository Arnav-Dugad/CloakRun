package com.arnavgames.cloakrun.feature.update

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Test

class ReleaseTest {
    private fun fixture(): String = checkNotNull(javaClass.classLoader?.getResource("github_latest_release.json")).readText()

    @Test
    fun semVer_parsesTagsAndBuildNames_andOrdersNumerically() {
        assertThat(SemVer.parse("v0.3.0")).isEqualTo(SemVer(0, 3, 0))
        assertThat(SemVer.parse("0.4.1-debug")).isEqualTo(SemVer(0, 4, 1))
        assertThat(SemVer.parse("nonsense")).isNull()
        assertThat(SemVer(0, 10, 0)).isGreaterThan(SemVer(0, 9, 9))
        assertThat(SemVer(1, 0, 0)).isGreaterThan(SemVer(0, 99, 99))
        assertThat(SemVer(0, 3, 0)).isEquivalentAccordingToCompareTo(SemVer(0, 3, 0))
    }

    @Test
    fun parser_readsTheRealGitHubResponse() {
        val release = ReleaseParser.parse(fixture())!!
        assertThat(release.version).isEqualTo(SemVer(0, 3, 0))
        assertThat(release.apkName).isEqualTo("CloakRun-v0.3.0.apk")
        assertThat(release.apkUrl).startsWith("https://github.com/Arnav-Dugad/CloakRun/releases/download/v0.3.0/")
        assertThat(release.apkSizeBytes).isGreaterThan(1_000_000L)
        assertThat(release.apkServerSha256).hasLength(64)
        assertThat(release.checksumsUrl).endsWith("CloakRun-v0.3.0-SHA256SUMS.txt")
        assertThat(release.notes).contains("### Added")
    }

    @Test
    fun parser_rejectsDraftsPrereleasesAndReleasesWithoutAnApk() {
        val base = fixture()
        assertThat(ReleaseParser.parse(base.replace("\"draft\": false", "\"draft\": true"))).isNull()
        assertThat(ReleaseParser.parse(base.replace("\"prerelease\": false", "\"prerelease\": true"))).isNull()
        assertThat(ReleaseParser.parse(base.replace("CloakRun-v0.3.0.apk", "something-else.apk"))).isNull()
        assertThat(ReleaseParser.parse("{not json")).isNull()
    }

    @Test
    fun checksums_findTheRightLine_andHashFiles() {
        val listing = """
            2b022fe5de960f3fc775911d85bc0d7abc33b4a62597fcb8ef40dd562a4de456  CloakRun-v0.3.0.apk
            6a6656fb71c6b66cc80b73cca5097ba569c26dbc44977af718501700f7271792  CloakRun-v0.3.0.aab
        """.trimIndent()
        assertThat(
            Checksums.find(listing, "CloakRun-v0.3.0.apk"),
        ).isEqualTo("2b022fe5de960f3fc775911d85bc0d7abc33b4a62597fcb8ef40dd562a4de456")
        assertThat(Checksums.find(listing, "CloakRun-v0.3.0.zip")).isNull()
        assertThat(Checksums.find("nothex  CloakRun-v0.3.0.apk", "CloakRun-v0.3.0.apk")).isNull()
        // SHA-256 of "abc", from FIPS 180-2.
        assertThat(Checksums.sha256(ByteArrayInputStream("abc".toByteArray())))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }

    @Test
    fun releaseNotes_keepWhatChanged_andDropHashesAndCommits() {
        val blocks = ReleaseNotes.parse(ReleaseParser.parse(fixture())!!.notes)
        assertThat(blocks.first()).isEqualTo(NoteBlock.Heading("Added"))
        assertThat(blocks.filterIsInstance<NoteBlock.Bullet>()).isNotEmpty()
        val text = blocks.joinToString(" ") { it.toString() }
        assertThat(text).doesNotContain("2b022fe5")
        assertThat(text).doesNotContain("Commits")
        assertThat(text).doesNotContain("**")
    }

    @Test
    fun releaseNotes_foldWrappedBulletLines() {
        val blocks = ReleaseNotes.parse("- first line\n  continues here\n- second")
        assertThat(blocks).containsExactly(
            NoteBlock.Bullet("first line continues here", 0),
            NoteBlock.Bullet("second", 0),
        ).inOrder()
    }

    @Test
    fun releaseNotes_paragraphAfterNestedListBelongsToTheParent() {
        val blocks = ReleaseNotes.parse(listOf("- Map:", "  - glowing roads", "  The style switches at sunset.").joinToString("\n"))
        assertThat(blocks).containsExactly(
            NoteBlock.Bullet("Map:", 0),
            NoteBlock.Bullet("glowing roads", 1),
            NoteBlock.Paragraph("The style switches at sunset."),
        ).inOrder()
    }
}
