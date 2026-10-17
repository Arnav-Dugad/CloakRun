package com.arnavgames.cloakrun.core.data

import com.arnavgames.cloakrun.core.game.lobby.LobbyPlayer
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Live lobby snapshots for one player. The match document is always readable; the players list is
 * readable only to members, so losing access to it means this player was removed or left.
 */
internal class LobbyWatcher(
    private val refs: LobbyRefs,
    private val matchId: String,
    private val uid: String,
) {
    fun updates(): Flow<LobbyUpdate> = callbackFlow {
        val state = State { trySend(it) }
        var playersListener: ListenerRegistration? = null
        val matchListener = refs.match(matchId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error.toLobbyException())
                return@addSnapshotListener
            }
            state.onMatch(snap)
            if (playersListener == null && !state.finished) {
                playersListener = refs.players(matchId).addSnapshotListener { ps, perr ->
                    if (perr != null) {
                        refs.match(matchId).get().addOnCompleteListener { task ->
                            state.onAccessLost(task.result?.takeIf { task.isSuccessful })
                        }
                    } else {
                        state.onPlayers(ps?.documents.orEmpty())
                    }
                }
            }
        }
        awaitClose {
            matchListener.remove()
            playersListener?.remove()
        }
    }

    /** Ended, Kicked and Left are final: a late snapshot must never bring a stale lobby back. */
    private inner class State(private val send: (LobbyUpdate) -> Unit) {
        var finished = false
            private set
        private var match: Map<String, Any?>? = null
        private var players: List<LobbyPlayer>? = null
        private var wasMember = false

        fun onMatch(snap: DocumentSnapshot?) {
            val data = snap?.takeIf { it.exists() }?.data
            if (data == null) finish(LobbyUpdate.Ended) else match = data
            emit()
        }

        fun onPlayers(docs: List<DocumentSnapshot>) {
            players = docs.mapNotNull { d -> d.data?.let { LobbyCodec.decodePlayer(d.id, it, ::millisOf) } }
            emit()
        }

        fun onAccessLost(snap: DocumentSnapshot?) {
            val data = snap?.takeIf { it.exists() }?.data
            finish(
                when {
                    data == null -> LobbyUpdate.Ended
                    kicked(data) -> LobbyUpdate.Kicked
                    else -> LobbyUpdate.Left
                },
            )
        }

        private fun emit() {
            val m = match ?: return
            val list = players ?: return
            when {
                finished -> Unit

                kicked(m) -> finish(LobbyUpdate.Kicked)

                list.none { it.uid == uid } -> if (wasMember) finish(LobbyUpdate.Left)

                else -> {
                    wasMember = true
                    LobbyCodec.decodeMatch(matchId, m, list)?.let { send(LobbyUpdate.Live(it, emptySet())) }
                }
            }
        }

        private fun kicked(m: Map<String, Any?>) = uid in (m["kicked"] as? List<*>).orEmpty()

        private fun finish(update: LobbyUpdate) {
            if (finished) return
            finished = true
            send(update)
        }
    }

    private companion object {
        /** Pending server timestamps sort last, so a player who just joined appears at the end. */
        fun millisOf(value: Any?): Long = (value as? Timestamp)?.toDate()?.time ?: Long.MAX_VALUE
    }
}
