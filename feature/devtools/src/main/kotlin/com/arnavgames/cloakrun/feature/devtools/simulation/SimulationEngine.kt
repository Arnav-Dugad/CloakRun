package com.arnavgames.cloakrun.feature.devtools.simulation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.geo.Geo
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.location.FixSource
import com.arnavgames.cloakrun.core.game.location.LocationFix
import com.arnavgames.cloakrun.core.game.sim.BotWalker
import com.arnavgames.cloakrun.core.game.sim.RoadGraph
import com.arnavgames.cloakrun.core.game.time.GameClock
import com.arnavgames.cloakrun.core.game.time.ScaledClock
import com.arnavgames.cloakrun.core.location.LocationRepository
import com.arnavgames.cloakrun.core.location.LocationSource
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

enum class SimTeam { Hider, Seeker }

@Immutable
data class SimBotView(
    val name: String,
    val team: SimTeam,
    val position: GeoPoint,
    val headingDeg: Double,
)

/**
 * Debug-only simulation: bots that walk the real street and path network, an optional simulated
 * "me" that replaces the device location, and a fast-forwardable clock. Never compiled into
 * release builds.
 */
class SimulationEngine(private val repository: LocationRepository) {
    private class Bot(
        val name: String,
        val team: SimTeam,
        val walker: BotWalker,
    )

    private val random = Random(SEED)
    var graph: RoadGraph? = null
        private set
    private var botList: List<Bot> = emptyList()
    private var me: BotWalker? = null

    val clock = ScaledClock(GameClock.System)

    var speed by mutableDoubleStateOf(1.0)
        private set

    var bots by mutableStateOf<List<SimBotView>>(emptyList())
        private set

    var walkingMe by mutableStateOf(false)
        private set

    val isActive: Boolean get() = botList.isNotEmpty() || walkingMe

    fun changeSpeed(value: Double) {
        speed = value
        clock.speed = value
    }

    /** Builds the walking graph from [lines] (visible streets and paths) and spawns [count] bots. */
    fun spawn(
        lines: List<List<GeoPoint>>,
        area: PlayArea?,
        count: Int = DEFAULT_BOTS,
    ): Boolean {
        val g = RoadGraph.build(lines)
        val component = g.largestComponent().toList()
        if (component.size < MIN_NODES) return false
        graph = g
        val inside = component.filter { area?.contains(g.nodes[it]) ?: true }.ifEmpty { component }
        val allowed: (GeoPoint) -> Boolean = { p -> area?.contains(p) ?: true }
        botList = (0 until count).map { i ->
            val team = if (i % 3 == 0) SimTeam.Seeker else SimTeam.Hider
            val speedMps = if (team == SimTeam.Seeker) 1.6 else 1.35
            Bot(NAMES[i % NAMES.size], team, BotWalker(g, Random(random.nextInt()), inside.random(random), speedMps, allowed))
        }
        publish()
        return true
    }

    fun clear() {
        botList = emptyList()
        setWalkMe(false)
        publish()
    }

    /** Replaces the device location with a bot that walks for you. */
    fun setWalkMe(enabled: Boolean) {
        val g = graph
        walkingMe = enabled && g != null
        if (walkingMe && g != null) {
            val start = g.largestComponent().random(random)
            me = BotWalker(g, Random(random.nextInt()), start, baseSpeedMps = 1.4)
            repository.sourceOverride.value = simulatedSource()
        } else {
            me = null
            repository.sourceOverride.value = null
        }
    }

    /**
     * Debug-only cheat: jumps the simulated you to a junction at least [SPOOF_JUMP_M] away, so the
     * route plausibility check can be seen flagging a teleport.
     */
    fun teleportMe() {
        val g = graph ?: return
        val here = me?.position ?: return
        val far = g.largestComponent().filter { Geo.distance(g.nodes[it], here) >= SPOOF_JUMP_M }
        if (far.isEmpty()) return
        me = BotWalker(g, Random(random.nextInt()), far.random(random), baseSpeedMps = 1.4)
    }

    /** Advances the simulation by [realSeconds] of wall time, scaled by the current speed. */
    fun tick(realSeconds: Double) {
        if (!isActive) return
        val dt = realSeconds * speed
        botList.forEach { it.walker.advance(dt) }
        me?.advance(dt)
        publish()
    }

    private fun publish() {
        bots = botList.map { SimBotView(it.name, it.team, it.walker.position, it.walker.headingDeg) }
    }

    private fun simulatedSource() = LocationSource { plan ->
        flow {
            while (true) {
                me?.let {
                    emit(
                        LocationFix(
                            point = it.position,
                            accuracyM = SIMULATED_ACCURACY_M,
                            timeMillis = clock.nowMillis(),
                            speedMps = if (it.isPaused) 0f else 1.4f,
                            bearingDeg = it.headingDeg.toFloat(),
                            source = FixSource.Simulated,
                        ),
                    )
                }
                delay((plan.intervalMs / speed).toLong().coerceAtLeast(MIN_FIX_INTERVAL_MS))
            }
        }
    }

    private companion object {
        const val SEED = 0x5EED
        const val DEFAULT_BOTS = 6
        const val MIN_NODES = 8
        const val SIMULATED_ACCURACY_M = 6f
        const val MIN_FIX_INTERVAL_MS = 200L
        const val SPOOF_JUMP_M = 300.0
        val NAMES = listOf("Nova", "Ash", "Kiran", "Mira", "Zed", "Juno", "Rio", "Tara")
    }
}
