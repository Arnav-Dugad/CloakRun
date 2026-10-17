package com.arnavgames.cloakrun.core.data

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.lobby.GameMode
import com.arnavgames.cloakrun.core.game.lobby.Lobby
import com.arnavgames.cloakrun.core.game.lobby.LobbyPlayer
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.game.lobby.MatchPhase
import com.arnavgames.cloakrun.core.game.lobby.Role
import kotlin.math.roundToLong

/**
 * Converts between the domain lobby and the Firestore field maps. Plain maps (no reflection) keep
 * R8 simple and make the wire format explicit; it must match firebase/firestore.rules.
 */
internal object LobbyCodec {
    fun encodeConfig(c: MatchConfig): Map<String, Any> = mapOf(
        "mode" to c.mode.wire,
        "seekers" to c.seekers.toLong(),
        "headStartSec" to c.headStartSeconds.toLong(),
        "huntMin" to c.huntMinutes.toLong(),
        "maxPlayers" to c.maxPlayers.toLong(),
    )

    fun decodeConfig(m: Map<*, *>?): MatchConfig? = runCatching {
        requireNotNull(m)
        MatchConfig(
            mode = GameMode.fromWire(m["mode"] as? String),
            seekers = m.int("seekers"),
            headStartSeconds = m.int("headStartSec"),
            huntMinutes = m.int("huntMin"),
            maxPlayers = m.int("maxPlayers"),
        )
    }.getOrNull()

    fun encodeArea(a: PlayArea): Map<String, Any> = when (a) {
        is PlayArea.Circle -> mapOf(
            "kind" to "circle",
            "lat" to round6(a.center.lat),
            "lng" to round6(a.center.lng),
            "radiusM" to Math.round(a.radiusM).toDouble(),
            "points" to emptyList<Double>(),
        )

        is PlayArea.Polygon -> mapOf(
            "kind" to "polygon",
            "lat" to round6(a.center.lat),
            "lng" to round6(a.center.lng),
            "radiusM" to 0.0,
            "points" to a.vertices.flatMap { listOf(round6(it.lat), round6(it.lng)) },
        )
    }

    fun decodeArea(m: Map<*, *>?): PlayArea? = runCatching {
        requireNotNull(m)
        when (m["kind"]) {
            "circle" -> PlayArea.Circle(GeoPoint(m.double("lat"), m.double("lng")), m.double("radiusM"))

            "polygon" -> {
                val flat = (m["points"] as List<*>).map { (it as Number).toDouble() }
                PlayArea.Polygon(flat.chunked(2).map { GeoPoint(it[0], it[1]) })
            }

            else -> null
        }
    }.getOrNull()

    fun encodePlayer(
        name: String,
        avatarSeed: String,
        joinedAt: Any,
        updatedAt: Any,
    ): Map<String, Any> = mapOf(
        "name" to name,
        "avatarSeed" to avatarSeed,
        "role" to Role.Unassigned.wire,
        "ready" to false,
        "briefed" to false,
        "joinedAt" to joinedAt,
        "updatedAt" to updatedAt,
    )

    /** [millis] reads a timestamp field as epoch milliseconds (Firestore Timestamps differ per platform). */
    fun decodePlayer(
        uid: String,
        m: Map<String, Any?>,
        millis: (Any?) -> Long,
    ): LobbyPlayer? {
        val name = m["name"] as? String ?: return null
        val seed = m["avatarSeed"] as? String ?: return null
        return LobbyPlayer(
            uid = uid,
            name = name,
            avatarSeed = seed,
            role = Role.fromWire(m["role"] as? String),
            ready = m["ready"] as? Boolean ?: false,
            joinedAtMillis = millis(m["joinedAt"]),
            briefed = m["briefed"] as? Boolean ?: false,
        )
    }

    fun decodeMatch(
        matchId: String,
        m: Map<String, Any?>,
        players: List<LobbyPlayer>,
    ): Lobby? = runCatching {
        Lobby(
            matchId = matchId,
            code = m["code"] as String,
            hostUid = m["hostUid"] as String,
            phase = requireNotNull(MatchPhase.fromWire(m["phase"] as? String)),
            config = requireNotNull(decodeConfig(m["config"] as? Map<*, *>)),
            area = requireNotNull(decodeArea(m["area"] as? Map<*, *>)),
            players = players,
            kicked = (m["kicked"] as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty(),
            playerCount = (m["playerCount"] as? Number)?.toInt() ?: players.size,
        )
    }.getOrNull()

    private fun round6(v: Double): Double = (v * 1e6).roundToLong() / 1e6

    private fun Map<*, *>.int(key: String): Int = (this[key] as Number).toInt()

    private fun Map<*, *>.double(key: String): Double = (this[key] as Number).toDouble()
}
