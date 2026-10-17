package com.arnavgames.cloakrun.core.data

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Delete my account and data": leaves (or ends) the current lobby, deletes the cloud profile and
 * the account, then forgets the profile on this phone. Location is never stored in the cloud, so
 * there is no location history to erase.
 */
@Singleton
class AccountDeletion @Inject constructor(
    private val backend: Backend,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val lobbies: LobbyRepository,
    private val prefs: PlayerPrefs,
) {
    /** Returns false if the player cancelled a required re-sign-in; nothing is deleted then. */
    suspend fun deleteEverything(activityContext: Context): Boolean {
        if (backend.available && auth.currentUid != null) {
            prefs.current().activeMatchId?.let { runCatching { lobbies.leave(it) } }
            profiles.deleteRemote()
            if (!auth.deleteAccount(activityContext)) return false
        }
        prefs.clearAccount()
        return true
    }
}
