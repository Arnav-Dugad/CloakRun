package com.arnavgames.cloakrun.core.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Realtime Database presence. `/lobbies/{match}` mirrors membership so the RTDB rules can check it
 * (they cannot read Firestore), and `/presence/{match}/{uid}` flips to offline automatically when a
 * phone drops its connection. Fuzzed zones (`/public`) arrive with the match engine in Phase 4.
 */
@Singleton
class PresenceRepository @Inject constructor(private val backend: Backend) {
    private val root: DatabaseReference get() = backend.database.reference

    private fun lobby(matchId: String) = root.child("lobbies").child(matchId)

    suspend fun registerHost(matchId: String, uid: String) {
        lobby(matchId).child("host").setValue(uid).await()
        registerMember(matchId, uid)
    }

    suspend fun registerMember(matchId: String, uid: String) {
        lobby(matchId).child("members").child(uid).setValue(true).await()
    }

    /** Online uids for a match, while collected. Marks this player online and arms the disconnect hook. */
    fun online(matchId: String, uid: String): Flow<Set<String>> = callbackFlow {
        val mine = root.child("presence").child(matchId).child(uid)
        val connected = root.child(".info/connected")
        val connection = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) == true) {
                    mine.onDisconnect().setValue(state(false))
                    mine.setValue(state(true))
                }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        val everyone = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    snapshot.children
                        .filter { it.child("online").getValue(Boolean::class.java) == true }
                        .mapNotNull { it.key }
                        .toSet(),
                )
            }

            // Presence is a nicety: losing it (for example after a kick) must not break the lobby.
            override fun onCancelled(error: DatabaseError) {
                trySend(emptySet())
            }
        }
        connected.addValueEventListener(connection)
        val list = root.child("presence").child(matchId)
        list.addValueEventListener(everyone)
        awaitClose {
            connected.removeEventListener(connection)
            list.removeEventListener(everyone)
            mine.onDisconnect().cancel()
            mine.setValue(state(false))
        }
    }

    suspend fun leave(matchId: String, uid: String) {
        runCatching {
            root.child("presence").child(matchId).child(uid).removeValue().await()
            lobby(matchId).child("members").child(uid).removeValue().await()
        }
    }

    suspend fun kick(matchId: String, uid: String) {
        runCatching {
            lobby(matchId).updateChildren(mapOf("kicked/$uid" to true, "members/$uid" to null)).await()
        }
    }

    /** Host only: removes zones, presence and membership for the match. */
    suspend fun clear(matchId: String) {
        runCatching {
            root.child("public").child(matchId).removeValue().await()
            root.child("presence").child(matchId).removeValue().await()
            lobby(matchId).removeValue().await()
        }
    }

    private fun state(online: Boolean) = mapOf("online" to online, "at" to ServerValue.TIMESTAMP)
}
