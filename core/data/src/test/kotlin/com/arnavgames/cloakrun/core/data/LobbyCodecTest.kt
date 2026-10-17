package com.arnavgames.cloakrun.core.data

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.game.lobby.MatchPhase
import com.arnavgames.cloakrun.core.game.lobby.Role
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LobbyCodecTest {
    @Test
    fun config_roundTrips_andMatchesTheRulesShape() {
        val config = MatchConfig(seekers = 2, headStartSeconds = 90, huntMinutes = 25, maxPlayers = 6)
        val wire = LobbyCodec.encodeConfig(config)
        assertThat(wire.keys).containsExactly("mode", "seekers", "headStartSec", "huntMin", "maxPlayers")
        assertThat(wire["mode"]).isEqualTo("classic")
        assertThat(LobbyCodec.decodeConfig(wire)).isEqualTo(config)
    }

    @Test
    fun circle_roundTrips_withRoundedCoordinates() {
        val area = PlayArea.Circle(GeoPoint(12.976312345, 77.592912345), 412.4)
        val wire = LobbyCodec.encodeArea(area)
        assertThat(wire["lat"]).isEqualTo(12.976312)
        assertThat(wire["radiusM"]).isEqualTo(412.0)
        assertThat(wire["points"] as List<*>).isEmpty()
        val back = LobbyCodec.decodeArea(wire) as PlayArea.Circle
        assertThat(back.radiusM).isEqualTo(412.0)
    }

    @Test
    fun polygon_flattensPoints_andDecodes() {
        val area = PlayArea.Polygon(listOf(GeoPoint(1.0, 2.0), GeoPoint(1.0, 2.01), GeoPoint(1.01, 2.01)))
        val wire = LobbyCodec.encodeArea(area)
        assertThat(wire["points"]).isEqualTo(listOf(1.0, 2.0, 1.0, 2.01, 1.01, 2.01))
        assertThat(wire["radiusM"]).isEqualTo(0.0)
        assertThat((LobbyCodec.decodeArea(wire) as PlayArea.Polygon).vertices).isEqualTo(area.vertices)
    }

    @Test
    fun badData_decodesToNull_insteadOfCrashing() {
        assertThat(LobbyCodec.decodeArea(mapOf("kind" to "hexagon"))).isNull()
        assertThat(LobbyCodec.decodeArea(mapOf("kind" to "polygon", "points" to listOf(1.0, 2.0)))).isNull()
        assertThat(LobbyCodec.decodeConfig(mapOf("seekers" to 0L))).isNull()
        assertThat(LobbyCodec.decodePlayer("u", mapOf("name" to "Ana"), millis = { 0L })).isNull()
    }

    @Test
    fun match_decodesPlayersRolesAndKicks() {
        val players = listOf(
            LobbyCodec.decodePlayer(
                "h",
                mapOf("name" to "Host Fox", "avatarSeed" to "abcdef", "role" to "seeker", "ready" to true, "briefed" to false),
                millis = { 5L },
            )!!,
        )
        val match = mapOf(
            "code" to "K7Q2XM",
            "hostUid" to "h",
            "phase" to "briefing",
            "config" to LobbyCodec.encodeConfig(MatchConfig()),
            "area" to LobbyCodec.encodeArea(PlayArea.Circle(GeoPoint(1.0, 2.0), 300.0)),
            "kicked" to listOf("x"),
            "playerCount" to 3L,
        )
        val lobby = LobbyCodec.decodeMatch("m", match, players)!!
        assertThat(lobby.phase).isEqualTo(MatchPhase.Briefing)
        assertThat(lobby.players.single().role).isEqualTo(Role.Seeker)
        assertThat(lobby.players.single().joinedAtMillis).isEqualTo(5L)
        assertThat(lobby.kicked).containsExactly("x")
        assertThat(lobby.playerCount).isEqualTo(3)
        assertThat(LobbyCodec.decodeMatch("m", match - "code", players)).isNull()
    }

    @Test
    fun newPlayer_isUnassignedAndNotReady() {
        val wire = LobbyCodec.encodePlayer("Ana", "abcdef", joinedAt = 1, updatedAt = 1)
        assertThat(wire["role"]).isEqualTo("unassigned")
        assertThat(wire["ready"]).isEqualTo(false)
        assertThat(wire["briefed"]).isEqualTo(false)
        assertThat(wire.keys).containsExactly("name", "avatarSeed", "role", "ready", "briefed", "joinedAt", "updatedAt")
    }
}
