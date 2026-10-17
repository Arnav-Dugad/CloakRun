package com.arnavgames.cloakrun.core.game.lobby

import com.arnavgames.cloakrun.core.game.area.PlayArea

/** Where a match is in its life. Only the host moves it forward; Security Rules check each step. */
enum class MatchPhase(val wire: String) {
    Lobby("lobby"),
    Briefing("briefing"),
    HeadStart("headstart"),
    Hunt("hunt"),
    Results("results"),
    Ended("ended"),
    ;

    companion object {
        fun fromWire(value: String?): MatchPhase? = entries.firstOrNull { it.wire == value }
    }
}

enum class Role(val wire: String) {
    Unassigned("unassigned"),
    Hider("hider"),
    Seeker("seeker"),
    Spectator("spectator"),
    ;

    companion object {
        fun fromWire(value: String?): Role = entries.firstOrNull { it.wire == value } ?: Unassigned
    }
}

enum class GameMode(val wire: String) {
    Classic("classic"),
    ;

    companion object {
        fun fromWire(value: String?): GameMode = entries.firstOrNull { it.wire == value } ?: Classic
    }
}

/** Host-chosen settings. Bounds are mirrored in the Firestore rules. */
data class MatchConfig(
    val mode: GameMode = GameMode.Classic,
    val seekers: Int = 1,
    val headStartSeconds: Int = 120,
    val huntMinutes: Int = 20,
    val maxPlayers: Int = MAX_PLAYERS,
) {
    init {
        require(seekers in 1 until MAX_PLAYERS) { "seekers out of range" }
        require(headStartSeconds in HEAD_START_RANGE) { "head start out of range" }
        require(huntMinutes in HUNT_RANGE) { "hunt length out of range" }
        require(maxPlayers in MIN_PLAYERS..MAX_PLAYERS) { "max players out of range" }
    }

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 8
        val HEAD_START_RANGE = 30..600
        val HUNT_RANGE = 5..60
    }
}

data class LobbyPlayer(
    val uid: String,
    val name: String,
    val avatarSeed: String,
    val role: Role = Role.Unassigned,
    val ready: Boolean = false,
    val joinedAtMillis: Long = 0L,
    /** Confirmed the pre-game briefing (role and safety rules). */
    val briefed: Boolean = false,
)

data class Lobby(
    val matchId: String,
    val code: String,
    val hostUid: String,
    val phase: MatchPhase,
    val config: MatchConfig,
    val area: PlayArea,
    val players: List<LobbyPlayer>,
    val kicked: Set<String> = emptySet(),
    /** The server-side count, which moves atomically with joins and leaves. */
    val playerCount: Int = players.size,
) {
    fun isHost(uid: String): Boolean = uid == hostUid

    fun player(uid: String): LobbyPlayer? = players.firstOrNull { it.uid == uid }

    val isFull: Boolean get() = playerCount >= config.maxPlayers

    /** Everyone has confirmed the briefing, so the host can release the head start. */
    val everyoneBriefed: Boolean get() = players.isNotEmpty() && players.all { it.briefed }

    /** Players in join order, host first, so the orbit and lists stay stable as people arrive. */
    val ordered: List<LobbyPlayer>
        get() = players.sortedWith(compareBy<LobbyPlayer>({ it.uid != hostUid }, { it.joinedAtMillis }, { it.uid }))
}

/** Why the host cannot start yet, in the order the host should fix them. */
enum class StartBlocker { NotEnoughPlayers, RolesUnassigned, NoSeeker, NoHider, NotEveryoneReady }

object StartCheck {
    fun blockers(lobby: Lobby): List<StartBlocker> = buildList {
        val players = lobby.players
        if (players.size < MatchConfig.MIN_PLAYERS) add(StartBlocker.NotEnoughPlayers)
        if (players.any { it.role == Role.Unassigned || it.role == Role.Spectator }) add(StartBlocker.RolesUnassigned)
        if (players.none { it.role == Role.Seeker }) add(StartBlocker.NoSeeker)
        if (players.none { it.role == Role.Hider }) add(StartBlocker.NoHider)
        // The host starting the match is their ready signal.
        if (players.any { !it.ready && it.uid != lobby.hostUid }) add(StartBlocker.NotEveryoneReady)
    }

    fun canStart(lobby: Lobby): Boolean = blockers(lobby).isEmpty()
}
