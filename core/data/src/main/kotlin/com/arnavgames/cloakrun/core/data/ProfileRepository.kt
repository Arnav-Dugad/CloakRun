package com.arnavgames.cloakrun.core.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * The player's name and avatar. The phone is the source of truth; the copy in `users/{uid}` lets
 * a Google-linked player restore it on another phone. No location is ever part of a profile.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val backend: Backend,
    private val auth: AuthRepository,
    private val prefs: PlayerPrefs,
) {
    // Uploads outlive the screen that asked for them.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Saves on the phone now; the cloud copy is uploaded in the background (and retried on next sync). */
    suspend fun save(profile: PlayerProfile) {
        prefs.setProfile(profile)
        if (backend.available) scope.launch { runCatching { upload(profile) } }
    }

    /** Pushes the local profile, for example right after signing in. */
    suspend fun sync() {
        val profile = prefs.current().profile ?: return
        if (backend.available) runCatching { upload(profile) }
    }

    /** After switching to an existing Google account, adopts that account's saved profile if it has one. */
    suspend fun restoreFromAccount(): PlayerProfile? {
        val uid = auth.currentUid ?: return null
        val doc = backend.firestore.collection(USERS).document(uid).get().await()
        val name = doc.getString("name") ?: return null
        val seed = doc.getString("avatarSeed") ?: return null
        return PlayerProfile(name, seed).also { prefs.setProfile(it) }
    }

    suspend fun deleteRemote() {
        val uid = auth.currentUid ?: return
        backend.firestore.collection(USERS).document(uid).delete().await()
    }

    private suspend fun upload(profile: PlayerProfile) {
        val uid = auth.ensureSignedIn()
        val ref = backend.firestore.collection(USERS).document(uid)
        val exists = ref.get().await().exists()
        val fields = mutableMapOf<String, Any>(
            "name" to profile.name,
            "avatarSeed" to profile.avatarSeed,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (!exists) fields["createdAt"] = FieldValue.serverTimestamp()
        ref.set(fields, SetOptions.merge()).await()
    }

    private companion object {
        const val USERS = "users"
    }
}
