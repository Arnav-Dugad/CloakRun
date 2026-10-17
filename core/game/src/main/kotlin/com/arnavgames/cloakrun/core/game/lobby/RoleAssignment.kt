package com.arnavgames.cloakrun.core.game.lobby

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Picks seekers for a match. Random, but fair over a session: anyone who sought last round goes to
 * the back of the queue, so the same person is not "it" twice in a row when others are available.
 */
object RoleAssignment {
    /** One seeker per four players, at least one, and always leaving at least one hider. */
    fun suggestedSeekers(playerCount: Int): Int = (playerCount / 4.0).roundToInt().coerceIn(1, (playerCount - 1).coerceAtLeast(1))

    fun assign(
        players: List<String>,
        seekers: Int,
        previousSeekers: Set<String> = emptySet(),
        random: Random = Random.Default,
    ): Map<String, Role> {
        require(players.size >= MatchConfig.MIN_PLAYERS) { "need at least ${MatchConfig.MIN_PLAYERS} players" }
        val count = seekers.coerceIn(1, players.size - 1)
        val (rested, sought) = players.shuffled(random).partition { it !in previousSeekers }
        val chosen = (rested + sought).take(count).toSet()
        return players.associateWith { if (it in chosen) Role.Seeker else Role.Hider }
    }
}
