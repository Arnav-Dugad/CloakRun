package com.arnavgames.cloakrun.core.game.time

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameClockTest {
    @Test
    fun scaledClock_runsFasterWithoutJumping() {
        val base = ManualClock(1_000)
        val clock = ScaledClock(base, speed = 1.0)
        base.advanceBy(1_000)
        assertThat(clock.nowMillis()).isEqualTo(2_000)
        clock.speed = 10.0
        assertThat(clock.nowMillis()).isEqualTo(2_000)
        base.advanceBy(1_000)
        assertThat(clock.nowMillis()).isEqualTo(12_000)
        clock.speed = 1.0
        base.advanceBy(500)
        assertThat(clock.nowMillis()).isEqualTo(12_500)
    }

    @Test(expected = IllegalArgumentException::class)
    fun scaledClock_rejectsNonPositiveSpeed() {
        ScaledClock(ManualClock()).speed = 0.0
    }
}
