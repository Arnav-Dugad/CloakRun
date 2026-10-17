package com.arnavgames.cloakrun.core.data

import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.lobby.JoinCode
import com.arnavgames.cloakrun.core.game.lobby.MatchConfig
import com.arnavgames.cloakrun.core.game.lobby.MatchPhase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Lobbies from a player's side: create, find, join, watch, ready up and leave. Joins and leaves are
 * transactions that move the player count, exactly as the Security Rules require. Host-only actions
 * live in [LobbyHostRepository].
 */
@Singleton
class LobbyRepository @Inject constructor(
    private val backend: Backend,
    private val auth: AuthRepository,
    private val prefs: PlayerPrefs,
    private val presence: PresenceRepository,
    private val flags: FeatureFlags,
    private val host: LobbyHostRepository,
) {
    private val refs by lazy { LobbyRefs(backend.firestore) }
    private val writeLock = Mutex()
    private var lastPlayerWrite = 0L

    /** Creates a lobby hosted by this player and returns the match id. A taken code is retried with a new one. */
    suspend fun create(
        area: PlayArea,
        config: MatchConfig,
        profile: PlayerProfile,
    ): String = call {
        val uid = auth.ensureSignedIn()
        var matchId: String? = null
        var attempts = 0
        while (matchId == null && attempts < CODE_ATTEMPTS) {
            attempts++
            matchId = tryCreate(uid, JoinCode.generate(), area, config, profile)
        }
        val created = matchId ?: throw LobbyException(LobbyFailure.Unknown)
        if (flags.rtdbEnabled) presence.registerHost(created, uid)
        prefs.setActiveMatch(created)
        created
    }

    private suspend fun tryCreate(
        uid: String,
        joinCode: String,
        area: PlayArea,
        config: MatchConfig,
        profile: PlayerProfile,
    ): String? {
        val ref = refs.newMatch()
        val now = FieldValue.serverTimestamp()
        val match = mapOf(
            "hostUid" to uid,
            "code" to joinCode,
            "phase" to MatchPhase.Lobby.wire,
            "config" to LobbyCodec.encodeConfig(config),
            "area" to LobbyCodec.encodeArea(area),
            "playerCount" to 1L,
            "kicked" to emptyList<String>(),
            "createdAt" to now,
            "updatedAt" to now,
            "phaseAt" to now,
        )
        val batch = backend.firestore.batch()
        batch.set(ref, match)
        batch.set(refs.player(ref.id, uid), LobbyCodec.encodePlayer(profile.name, profile.avatarSeed, now, now))
        batch.set(refs.code(joinCode), mapOf("matchId" to ref.id, "hostUid" to uid, "createdAt" to now))
        return try {
            batch.commit().await()
            ref.id
        } catch (e: FirebaseFirestoreException) {
            // The only expected denial is a code that is already taken.
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) null else throw e
        }
    }

    /** Resolves a code to a match id. */
    suspend fun find(joinCode: String): String = call {
        auth.ensureSignedIn()
        val matchId = refs.code(joinCode).get().await().getString("matchId")
        val exists = matchId != null && refs.match(matchId).get().await().exists()
        if (!exists) throw LobbyException(LobbyFailure.CodeNotFound)
        requireNotNull(matchId)
    }

    /** Joins (or rejoins) a lobby. */
    suspend fun join(matchId: String, profile: PlayerProfile) = call {
        val uid = auth.ensureSignedIn()
        if (!refs.player(matchId, uid).get().await().exists()) {
            backend.firestore.runTransaction { tx ->
                val m = tx.get(refs.match(matchId))
                joinBlocker(m, uid)?.let { throw LobbyException(it) }
                val now = FieldValue.serverTimestamp()
                tx.update(refs.match(matchId), "playerCount", (m.getLong("playerCount") ?: 0L) + 1)
                tx.set(refs.player(matchId, uid), LobbyCodec.encodePlayer(profile.name, profile.avatarSeed, now, now))
            }.await()
        }
        if (flags.rtdbEnabled) presence.registerMember(matchId, uid)
        prefs.setActiveMatch(matchId)
    }

    /** Why this player cannot join, checked before writing so the message is precise. The rules check it too. */
    private fun joinBlocker(m: DocumentSnapshot, uid: String): LobbyFailure? {
        val count = m.getLong("playerCount") ?: 0L
        val max = ((m.get("config") as? Map<*, *>)?.get("maxPlayers") as? Number)?.toLong() ?: Long.MAX_VALUE
        val created = m.getTimestamp("createdAt")
        return when {
            !m.exists() -> LobbyFailure.CodeNotFound
            uid in (m.get("kicked") as? List<*>).orEmpty() -> LobbyFailure.Kicked
            m.getString("phase") != MatchPhase.Lobby.wire -> LobbyFailure.AlreadyStarted
            created != null && Timestamp.now().seconds - created.seconds > LobbyRefs.EXPIRY_S -> LobbyFailure.Expired
            count >= max -> LobbyFailure.Full
            else -> null
        }
    }

    /** Live lobby updates with who is online. */
    fun observe(matchId: String): Flow<LobbyUpdate> {
        if (!backend.available) return flowOf(LobbyUpdate.Ended)
        val uid = auth.currentUid ?: return flowOf(LobbyUpdate.Left)
        val online = if (flags.rtdbEnabled) presence.online(matchId, uid) else flowOf(emptySet())
        return combine(LobbyWatcher(refs, matchId, uid).updates(), online) { update, here ->
            if (update is LobbyUpdate.Live) update.copy(online = here + uid) else update
        }.distinctUntilChanged()
    }

    suspend fun setReady(matchId: String, ready: Boolean) = updateSelf(matchId, mapOf("ready" to ready))

    suspend fun confirmBriefing(matchId: String) = updateSelf(matchId, mapOf("briefed" to true))

    /** Leaves a lobby. A host leaving ends it for everyone. */
    suspend fun leave(matchId: String) = call {
        val uid = auth.ensureSignedIn()
        val m = refs.match(matchId).get().await()
        if (m.exists() && m.getString("hostUid") == uid) {
            host.end(matchId)
        } else if (m.exists()) {
            backend.firestore.runTransaction { tx ->
                val snap = tx.get(refs.match(matchId))
                val mine = tx.get(refs.player(matchId, uid))
                if (snap.exists() && mine.exists()) {
                    tx.update(refs.match(matchId), "playerCount", (snap.getLong("playerCount") ?: 1L) - 1)
                    tx.delete(refs.player(matchId, uid))
                }
            }.await()
            presence.leave(matchId, uid)
        }
        prefs.setActiveMatch(null)
    }

    /** Own-document writes are rate limited by the rules (400 ms), so they are spaced out here. */
    private suspend fun updateSelf(matchId: String, fields: Map<String, Any>) = call {
        val uid = auth.ensureSignedIn()
        writeLock.withLock {
            val wait = SELF_WRITE_GAP_MS - (System.currentTimeMillis() - lastPlayerWrite)
            if (wait > 0) delay(wait)
            refs.player(matchId, uid).update(fields + ("updatedAt" to FieldValue.serverTimestamp())).await()
            lastPlayerWrite = System.currentTimeMillis()
        }
    }

    private suspend fun <T> call(block: suspend () -> T): T = lobbyCall(backend, flags, block)

    private companion object {
        const val CODE_ATTEMPTS = 5
        const val SELF_WRITE_GAP_MS = 600L
    }
}
