package com.arnavgames.cloakrun.core.game.location

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SamplingPolicyTest {
    private fun plan(input: SamplingInput) = SamplingPolicy.plan(input)!!

    @Test
    fun off_neverRequestsLocation() {
        assertThat(SamplingPolicy.plan(SamplingInput(TrackingMode.Off))).isNull()
    }

    @Test
    fun match_speedsUpAsOpponentsCloseIn() {
        val far = plan(SamplingInput(TrackingMode.Match, nearestOpponentM = 800.0))
        val near = plan(SamplingInput(TrackingMode.Match, nearestOpponentM = 150.0))
        val close = plan(SamplingInput(TrackingMode.Match, nearestOpponentM = 40.0))
        assertThat(far.intervalMs).isGreaterThan(near.intervalMs)
        assertThat(near.intervalMs).isGreaterThan(close.intervalMs)
        assertThat(close.intervalMs).isEqualTo(1_000L)
        assertThat(close.priority).isEqualTo(FixPriority.HighAccuracy)
    }

    @Test
    fun lowBattery_backsOffUnlessTheChaseIsClose() {
        val normal = plan(SamplingInput(TrackingMode.Match))
        val low = plan(SamplingInput(TrackingMode.Match, batteryPercent = 15))
        val lowButClose = plan(SamplingInput(TrackingMode.Match, batteryPercent = 15, nearestOpponentM = 30.0))
        assertThat(low.intervalMs).isGreaterThan(normal.intervalMs)
        assertThat(low.priority).isEqualTo(FixPriority.Balanced)
        assertThat(lowButClose.intervalMs).isEqualTo(1_000L)
    }

    @Test
    fun criticalBattery_neverPollsFasterThanFiveSeconds() {
        val critical = plan(SamplingInput(TrackingMode.Match, batteryPercent = 5, nearestOpponentM = 20.0))
        assertThat(critical.intervalMs).isAtLeast(5_000L)
        assertThat(critical.priority).isEqualTo(FixPriority.Balanced)
    }

    @Test
    fun charging_ignoresBatteryLevel() {
        val charging = plan(SamplingInput(TrackingMode.Match, batteryPercent = 5, charging = true))
        assertThat(charging).isEqualTo(plan(SamplingInput(TrackingMode.Match)))
    }

    @Test
    fun stationaryAndScreenOff_saveMorePower() {
        val moving = plan(SamplingInput(TrackingMode.Match))
        val still = plan(SamplingInput(TrackingMode.Match, stationary = true, screenOn = false))
        assertThat(still.intervalMs).isGreaterThan(moving.intervalMs)
        assertThat(still.maxBatchDelayMs).isGreaterThan(0L)
    }

    @Test
    fun everyPlan_isSane() {
        for (mode in TrackingMode.entries.filter { it != TrackingMode.Off }) {
            for (battery in listOf(3, 15, 60, 100)) {
                for (distance in listOf(null, 10.0, 150.0, 2_000.0)) {
                    for (saver in listOf(false, true)) {
                        val p = plan(SamplingInput(mode, distance, battery, powerSaveMode = saver))
                        assertThat(p.intervalMs).isIn(500L..20_000L)
                        assertThat(p.minIntervalMs).isAtMost(p.intervalMs)
                    }
                }
            }
        }
    }
}
