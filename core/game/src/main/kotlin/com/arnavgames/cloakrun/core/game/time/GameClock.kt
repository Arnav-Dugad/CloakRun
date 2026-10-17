package com.arnavgames.cloakrun.core.game.time

/**
 * The only source of time for game rules. Production uses the device clock; Simulation Mode
 * swaps in a [ScaledClock] to fast-forward matches, and tests use a manual clock.
 */
fun interface GameClock {
    fun nowMillis(): Long

    companion object {
        val System = GameClock { java.lang.System.currentTimeMillis() }
    }
}

/**
 * A clock that runs [speed] times faster than [base], starting from the base time when created.
 * Changing [speed] keeps time continuous (no jumps backwards).
 */
class ScaledClock(private val base: GameClock, speed: Double = 1.0) : GameClock {
    private var anchorBase = base.nowMillis()
    private var anchorScaled = anchorBase.toDouble()

    var speed: Double = speed
        set(value) {
            require(value > 0) { "speed must be positive" }
            val now = base.nowMillis()
            anchorScaled += (now - anchorBase) * field
            anchorBase = now
            field = value
        }

    override fun nowMillis(): Long = (anchorScaled + (base.nowMillis() - anchorBase) * speed).toLong()
}

/** A clock that only moves when told to. For tests. */
class ManualClock(private var now: Long = 0L) : GameClock {
    override fun nowMillis(): Long = now

    fun advanceBy(ms: Long) {
        now += ms
    }
}
