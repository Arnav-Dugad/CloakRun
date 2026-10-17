package com.arnavgames.cloakrun.core.data

import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.game.lobby.MatchPhase
import com.arnavgames.cloakrun.core.game.lobby.Role
import com.google.firebase.firestore.FieldValue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/** What only the host may do: settings, roles, phases, removing players and ending the lobby. */
@Singleton
class LobbyHostRepository @Inject constructor(
    private val backend: Backend,
    private val prefs: PlayerPrefs,
    private val presence: PresenceRepository,
    private val flags: FeatureFlags,
) {
    private val refs by lazy { LobbyRefs(backend.firestore) }

    suspend fun updateConfig(matchId: String, config: MatchConfig) = call {
        refs.match(matchId)
            .update(mapOf("config" to LobbyCodec.encodeConfig(config), "updatedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun assignRoles(matchId: String, roles: Map<String, Role>) = call {
        val batch = backend.firestore.batch()
        roles.forEach { (uid, role) ->
            batch.update(refs.player(matchId, uid), mapOf("role" to role.wire, "updatedAt" to FieldValue.serverTimestamp()))
        }
        batch.commit().await()
    }

    /** Removes a player for good: the count, the kicked list and their document change together. */
    suspend fun kick(matchId: String, uid: String) = call {
        backend.firestore.runTransaction { tx ->
            val m = tx.get(refs.match(matchId))
            val kicked = (m.get("kicked") as? List<*>).orEmpty().filterIsInstance<String>()
            if (uid !in kicked) {
                tx.update(
                    refs.match(matchId),
                    mapOf(
                        "kicked" to kicked + uid,
                        "playerCount" to (m.getLong("playerCount") ?: 1L) - 1,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.delete(refs.player(matchId, uid))
            }
        }.await()
        if (flags.rtdbEnabled) presence.kick(matchId, uid)
    }

    suspend fun setPhase(matchId: String, phase: MatchPhase) = call {
        val now = FieldValue.serverTimestamp()
        refs.match(matchId).update(mapOf("phase" to phase.wire, "phaseAt" to now, "updatedAt" to now)).await()
    }

    /** Back to the lobby after a briefing or a match: phase first, then everyone's readiness resets. */
    suspend fun backToLobby(matchId: String, playerUids: List<String>) = call {
        setPhase(matchId, MatchPhase.Lobby)
        val batch = backend.firestore.batch()
        playerUids.forEach {
            batch.update(
                refs.player(matchId, it),
                mapOf("ready" to false, "briefed" to false, "updatedAt" to FieldValue.serverTimestamp()),
            )
        }
        batch.commit().await()
    }

    /** Ends the lobby: every document goes in one batch, then the presence data. */
    suspend fun end(matchId: String) = call {
        val m = refs.match(matchId).get().await()
        if (m.exists()) {
            val players = refs.players(matchId).get().await()
            val batch = backend.firestore.batch()
            players.documents.forEach { batch.delete(it.reference) }
            m.getString("code")?.let { batch.delete(refs.code(it)) }
            batch.delete(refs.match(matchId))
            batch.commit().await()
        }
        if (flags.rtdbEnabled) presence.clear(matchId)
        prefs.setActiveMatch(null)
    }

    private suspend fun <T> call(block: suspend () -> T): T = lobbyCall(backend, flags, block)
}
