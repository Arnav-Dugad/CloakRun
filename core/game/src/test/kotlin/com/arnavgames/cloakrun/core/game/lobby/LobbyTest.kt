package com.arnavgames.cloakrun.core.game.lobby

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.geo.GeoPoint
import com.arnavgames.cloakrun.core.game.profile.DisplayName
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

class LobbyTest {
    @Test
    fun joinCode_generatesOnlyUnambiguousCharacters() {
        val random = Random(7)
        repeat(500) {
            val code = JoinCode.generate(random)
            assertThat(JoinCode.isValid(code)).isTrue()
            assertThat(code.none { it in "01OIL" }).isTrue()
        }
    }

    @Test
    fun joinCode_normalizesTypedInput_andRejectsLookAlikes() {
        assertThat(JoinCode.normalize(" k7q-2xm ")).isEqualTo("K7Q2XM")
        assertThat(JoinCode.normalize("k7q 2xm")).isEqualTo("K7Q2XM")
        // O, 0, I, L and 1 never appear in codes, so a code containing them is a typo, not a guess.
        assertThat(JoinCode.normalize("K7Q2XO")).isNull()
        assertThat(JoinCode.normalize("k7q-2xmz")).isNull()
        assertThat(JoinCode.normalize("ABC10D")).isNull()
        assertThat(JoinCode.filterTyping("ab-c1o9xyz")).isEqualTo("ABC9XY")
    }

    @Test
    fun inviteLink_roundTrips_andIgnoresOtherHosts() {
        val link = InviteLink.forCode("K7Q2XM")
        assertThat(link).isEqualTo("https://arnav-dugad.github.io/join/?code=K7Q2XM")
        assertThat(InviteLink.parse(link)).isEqualTo("K7Q2XM")
        assertThat(InviteLink.parse("https://arnav-dugad.github.io/join?code=k7q2xm&utm=qr#x")).isEqualTo("K7Q2XM")
        assertThat(InviteLink.parse("k7q2xm")).isEqualTo("K7Q2XM")
        assertThat(InviteLink.parse("https://evil.example/join/?code=K7Q2XM")).isNull()
        assertThat(InviteLink.parse("https://arnav-dugad.github.io.evil.example/join/?code=K7Q2XM")).isNull()
        assertThat(InviteLink.parse("https://arnav-dugad.github.io/join/?code=NOPE")).isNull()
    }

    @Test
    fun roles_assignRequestedSeekers_andAlwaysLeaveAHider() {
        val players = listOf("a", "b", "c", "d", "e")
        val roles = RoleAssignment.assign(players, seekers = 2, random = Random(1))
        assertThat(roles.keys).containsExactlyElementsIn(players)
        assertThat(roles.values.count { it == Role.Seeker }).isEqualTo(2)
        val tooMany = RoleAssignment.assign(listOf("a", "b"), seekers = 5, random = Random(1))
        assertThat(tooMany.values.count { it == Role.Hider }).isEqualTo(1)
    }

    @Test
    fun roles_lastRoundSeekersRestWhenOthersAreAvailable() {
        val players = listOf("a", "b", "c", "d")
        repeat(50) { seed ->
            val roles = RoleAssignment.assign(players, seekers = 1, previousSeekers = setOf("a", "b"), random = Random(seed))
            assertThat(roles.filterValues { it == Role.Seeker }.keys).containsAnyOf("c", "d")
        }
    }

    @Test
    fun suggestedSeekers_scalesWithLobbySize() {
        assertThat(RoleAssignment.suggestedSeekers(2)).isEqualTo(1)
        assertThat(RoleAssignment.suggestedSeekers(5)).isEqualTo(1)
        assertThat(RoleAssignment.suggestedSeekers(6)).isEqualTo(2)
        assertThat(RoleAssignment.suggestedSeekers(8)).isEqualTo(2)
    }

    @Test
    fun startCheck_listsEveryBlocker_andClearsWhenReady() {
        val host = LobbyPlayer("h", "Host", "s1", joinedAtMillis = 1)
        val guest = LobbyPlayer("g", "Guest", "s2", joinedAtMillis = 2)
        val lobby = Lobby(
            matchId = "m",
            code = "K7Q2XM",
            hostUid = "h",
            phase = MatchPhase.Lobby,
            config = MatchConfig(),
            area = PlayArea.Circle(GeoPoint(12.97, 77.59), 300.0),
            players = listOf(host),
        )
        assertThat(StartCheck.blockers(lobby)).containsExactly(
            StartBlocker.NotEnoughPlayers,
            StartBlocker.RolesUnassigned,
            StartBlocker.NoSeeker,
            StartBlocker.NoHider,
        ).inOrder()
        val assigned = lobby.copy(players = listOf(host.copy(role = Role.Seeker), guest.copy(role = Role.Hider)))
        assertThat(StartCheck.blockers(assigned)).containsExactly(StartBlocker.NotEveryoneReady)
        val ready = assigned.copy(players = assigned.players.map { it.copy(ready = it.uid == "g") })
        assertThat(StartCheck.canStart(ready)).isTrue()
        assertThat(ready.ordered.map { it.uid }).containsExactly("h", "g").inOrder()
    }

    @Test
    fun displayName_validatesAndGenerates() {
        assertThat(DisplayName.problem("A")).isEqualTo(DisplayName.Problem.TooShort)
        assertThat(DisplayName.problem("x".repeat(21))).isEqualTo(DisplayName.Problem.TooLong)
        assertThat(DisplayName.problem(" Ana")).isEqualTo(DisplayName.Problem.BadCharacters)
        assertThat(DisplayName.problem("Ana<script>")).isEqualTo(DisplayName.Problem.BadCharacters)
        assertThat(DisplayName.problem("Ana_B-2.0")).isNull()
        assertThat(DisplayName.problem("Ananya Rao")).isNull()
        repeat(100) { assertThat(DisplayName.problem(DisplayName.generate(Random(it)))).isNull() }
        assertThat(DisplayName.clean("  Ana   Rao ")).isEqualTo("Ana Rao")
    }

    @Test
    fun matchConfig_rejectsOutOfRangeSettings() {
        assertThat(runCatching { MatchConfig(seekers = 0) }.isFailure).isTrue()
        assertThat(runCatching { MatchConfig(headStartSeconds = 5) }.isFailure).isTrue()
        assertThat(runCatching { MatchConfig(huntMinutes = 120) }.isFailure).isTrue()
        assertThat(MatchPhase.fromWire("hunt")).isEqualTo(MatchPhase.Hunt)
        assertThat(Role.fromWire("nonsense")).isEqualTo(Role.Unassigned)
    }
}
