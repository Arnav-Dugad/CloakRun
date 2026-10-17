package com.arnavgames.cloakrun.core.data

import com.arnavgames.cloakrun.core.game.lobby.Lobby
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

enum class LobbyFailure { NotConfigured, Disabled, CodeNotFound, Full, AlreadyStarted, Expired, Kicked, Offline, Denied, Unknown }

class LobbyException(val failure: LobbyFailure, cause: Throwable? = null) : Exception(failure.name, cause)

/** What a lobby screen sees: the live lobby, or why it is gone. */
sealed interface LobbyUpdate {
    data class Live(val lobby: Lobby, val online: Set<String>) : LobbyUpdate

    /** The host ended the lobby, or it expired. */
    data object Ended : LobbyUpdate

    data object Kicked : LobbyUpdate

    data object Left : LobbyUpdate
}

/** Firestore paths for lobbies. They must match firebase/firestore.rules. */
internal class LobbyRefs(private val db: FirebaseFirestore) {
    fun match(id: String) = db.collection(MATCHES).document(id)

    fun newMatch() = db.collection(MATCHES).document()

    fun players(matchId: String) = match(matchId).collection(PLAYERS)

    fun player(matchId: String, uid: String) = players(matchId).document(uid)

    fun code(code: String) = db.collection(CODES).document(code)

    companion object {
        const val MATCHES = "matches"
        const val PLAYERS = "players"
        const val CODES = "joinCodes"

        /** Lobbies expire six hours after creation (enforced by the rules). */
        const val EXPIRY_S = 6L * 60 * 60
    }
}

/** Runs a lobby operation, turning every Firebase failure into a [LobbyException] the UI can explain. */
internal suspend fun <T> lobbyCall(
    backend: Backend,
    flags: FeatureFlags,
    block: suspend () -> T,
): T {
    val unavailable = when {
        !backend.available -> LobbyFailure.NotConfigured
        !flags.matchesEnabled -> LobbyFailure.Disabled
        else -> null
    }
    if (unavailable != null) throw LobbyException(unavailable)
    return try {
        block()
    } catch (e: FirebaseException) {
        // A LobbyException thrown inside a transaction arrives either as itself or as the cause.
        throw e.toLobbyException()
    }
}

internal fun Throwable.toLobbyException(): LobbyException = when (this) {
    is LobbyException -> this

    // Transactions wrap exceptions thrown inside them.
    is FirebaseFirestoreException -> (cause as? LobbyException) ?: LobbyException(code.toFailure(), this)

    is FirebaseNetworkException -> LobbyException(LobbyFailure.Offline, this)

    else -> (cause as? LobbyException) ?: LobbyException(LobbyFailure.Unknown, this)
}

private fun FirebaseFirestoreException.Code.toFailure(): LobbyFailure = when (this) {
    FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> LobbyFailure.Offline
    FirebaseFirestoreException.Code.PERMISSION_DENIED -> LobbyFailure.Denied
    FirebaseFirestoreException.Code.NOT_FOUND -> LobbyFailure.CodeNotFound
    else -> LobbyFailure.Unknown
}
