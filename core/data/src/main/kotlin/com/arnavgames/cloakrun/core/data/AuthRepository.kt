package com.arnavgames.cloakrun.core.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val isAnonymous: Boolean,
    val email: String?,
)

sealed interface GoogleLinkResult {
    data class Linked(val user: AuthUser) : GoogleLinkResult

    /**
     * That Google account already has a CloakRun account. Switching to it keeps that account's
     * profile; the current guest account is left behind.
     */
    data class AlreadyInUse(internal val credential: AuthCredential) : GoogleLinkResult

    data object Cancelled : GoogleLinkResult

    data object NoGoogleAccount : GoogleLinkResult

    data class Failed(val reason: String) : GoogleLinkResult
}

/**
 * Accounts. Everyone starts as an anonymous guest, so play needs no sign-up. Linking Google keeps
 * the same uid (and profile) and makes it recoverable on another phone.
 */
@Singleton
class AuthRepository @Inject constructor(private val backend: Backend) {
    private val auth: FirebaseAuth get() = backend.auth

    val user: Flow<AuthUser?> = if (!backend.available) {
        flowOf(null)
    } else {
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    val currentUid: String? get() = if (backend.available) auth.currentUser?.uid else null

    /** Returns the signed-in uid, signing in anonymously first if needed. */
    suspend fun ensureSignedIn(): String {
        check(backend.available) { "Online play is not configured in this build" }
        auth.currentUser?.let { return it.uid }
        return requireNotNull(auth.signInAnonymously().await().user).uid
    }

    /** Shows the system Google account picker and links the chosen account to this player. */
    suspend fun linkGoogle(activityContext: Context): GoogleLinkResult {
        val credential = when (val token = googleIdToken(activityContext)) {
            is TokenResult.Token -> GoogleAuthProvider.getCredential(token.idToken, null)
            is TokenResult.Done -> return token.result
        }
        ensureSignedIn()
        val current = requireNotNull(auth.currentUser)
        return try {
            val linked = current.linkWithCredential(credential).await().user ?: current
            GoogleLinkResult.Linked(linked.toAuthUser())
        } catch (e: FirebaseAuthUserCollisionException) {
            GoogleLinkResult.AlreadyInUse(e.updatedCredential ?: credential)
        }
    }

    /** Signs in to the existing account behind [result], leaving the guest account. */
    suspend fun switchTo(result: GoogleLinkResult.AlreadyInUse): AuthUser {
        val user = requireNotNull(auth.signInWithCredential(result.credential).await().user)
        return user.toAuthUser()
    }

    /**
     * Deletes the Firebase account. Google accounts may need a fresh sign-in first; the picker is
     * shown again in that case. Returns false if the player cancelled it.
     */
    suspend fun deleteAccount(activityContext: Context): Boolean {
        val user = auth.currentUser ?: return true
        try {
            user.delete().await()
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            val token = googleIdToken(activityContext) as? TokenResult.Token ?: return false
            user.reauthenticate(GoogleAuthProvider.getCredential(token.idToken, null)).await()
            user.delete().await()
        }
        return true
    }

    fun signOut() {
        if (backend.available) auth.signOut()
    }

    private sealed interface TokenResult {
        data class Token(val idToken: String) : TokenResult

        data class Done(val result: GoogleLinkResult) : TokenResult
    }

    private suspend fun googleIdToken(activityContext: Context): TokenResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(BackendConfig.WEB_CLIENT_ID).build())
            .build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                TokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                TokenResult.Done(GoogleLinkResult.Failed("Unexpected credential type"))
            }
        } catch (_: GetCredentialCancellationException) {
            TokenResult.Done(GoogleLinkResult.Cancelled)
        } catch (_: NoCredentialException) {
            TokenResult.Done(GoogleLinkResult.NoGoogleAccount)
        } catch (e: GetCredentialException) {
            TokenResult.Done(GoogleLinkResult.Failed(e.type))
        }
    }

    private fun com.google.firebase.auth.FirebaseUser.toAuthUser() = AuthUser(uid, isAnonymous, email)
}
