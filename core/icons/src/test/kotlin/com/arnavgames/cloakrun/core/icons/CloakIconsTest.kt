package com.arnavgames.cloakrun.core.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class CloakIconsTest {
    private fun VectorNode.paths(): List<VectorPath> = when (this) {
        is VectorPath -> listOf(this)
        is VectorGroup -> flatMap { it.paths() }
        else -> emptyList()
    }

    @Test
    fun requiredIconsFromTheBrief_allExist() {
        val names = CloakIcons.all.map { it.first }.toSet()
        val required = listOf(
            "radar", "cloak", "decoy", "sonar", "freeze", "trap", "cache", "zone", "tag", "hider", "seeker",
            "ghost", "trophy", "lobby", "invite", "qr", "sos", "battery", "signal", "compass", "settings",
            "profile", "replay", "shield", "eye", "eye off", "timer", "team",
        )
        assertThat(names).containsAtLeastElementsIn(required)
    }

    @Test
    fun everyIcon_isOnThe24Grid_withA2dpRoundStroke() {
        for ((name, vector) in CloakIcons.all) {
            assertWithMessage("$name viewport").that(vector.viewportWidth).isEqualTo(ICON_GRID)
            assertWithMessage("$name viewport").that(vector.viewportHeight).isEqualTo(ICON_GRID)
            val paths = vector.root.paths()
            assertWithMessage("$name has paths").that(paths).isNotEmpty()
            for (p in paths.filter { it.stroke != null }) {
                assertWithMessage("$name stroke width").that(p.strokeLineWidth).isEqualTo(ICON_STROKE)
                assertWithMessage("$name cap").that(p.strokeLineCap).isEqualTo(StrokeCap.Round)
                assertWithMessage("$name join").that(p.strokeLineJoin).isEqualTo(StrokeJoin.Round)
            }
        }
    }

    @Test
    fun iconNames_areUnique() {
        val names = CloakIcons.all.map { it.first }
        assertThat(names).containsNoDuplicates()
        assertThat(CloakIcons.all.map { it.second.name }).containsNoDuplicates()
    }

    @Test
    fun directionalIcons_mirrorInRtl() {
        for (vector in listOf(CloakIcons.Back, CloakIcons.ChevronRight, CloakIcons.Replay, CloakIcons.Play)) {
            assertWithMessage(vector.name).that(vector.autoMirror).isTrue()
        }
    }
}
