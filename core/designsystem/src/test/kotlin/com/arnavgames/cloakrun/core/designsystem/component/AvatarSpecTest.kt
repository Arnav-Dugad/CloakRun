package com.arnavgames.cloakrun.core.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AvatarSpecTest {
    @Test
    fun sameSeed_alwaysProducesTheSameAvatar() {
        assertThat(AvatarSpec.from("player-42")).isEqualTo(AvatarSpec.from("player-42"))
    }

    @Test
    fun hash_isStableAcrossVersions() {
        // FNV-1a 64-bit of "a" is a published constant; if this changes, every avatar changes.
        assertThat(AvatarSpec.hash("a")).isEqualTo(0xaf63dc4c8601ec8cuL.toLong())
    }

    @Test
    fun differentSeeds_spreadAcrossPatterns() {
        val patterns = (1..200).map { AvatarSpec.from("seed-$it").pattern }.toSet()
        assertThat(patterns).containsExactlyElementsIn(AvatarPattern.entries)
    }

    @Test
    fun hues_neverLandOnTeamColours() {
        for (h in 0 until 360) {
            val adjusted = AvatarSpec.avoidTeamHues(h.toFloat())
            assertThat(adjusted !in 20f..45f && adjusted !in 172f..200f).isTrue()
        }
    }
}
