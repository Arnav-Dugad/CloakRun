package com.arnavgames.cloakrun.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playerStore: DataStore<Preferences> by preferencesDataStore(name = "player")

/** Age bands from the age gate. Only the band is stored, never a birth date. */
enum class AgeBand { Under13, Teen, Adult }

data class PlayerProfile(val name: String, val avatarSeed: String)

data class PlayerState(
    val ageBand: AgeBand?,
    val rulesAccepted: Boolean,
    val consentVersion: Int,
    val diagnostics: Boolean,
    val profile: PlayerProfile?,
    val activeMatchId: String?,
) {
    /** Onboarding is done once the age gate passed, the rules and consent are accepted and a name exists. */
    val onboarded: Boolean
        get() = (ageBand == AgeBand.Teen || ageBand == AgeBand.Adult) && rulesAccepted &&
            consentVersion >= CURRENT_CONSENT && profile != null

    companion object {
        /** Bump when what is shared changes; everyone sees the consent screen again. */
        const val CURRENT_CONSENT = 1
    }
}

/** On-device player settings. The profile is mirrored to Firestore once signed in. */
@Singleton
class PlayerPrefs @Inject constructor(@param:ApplicationContext private val context: Context) {
    private object Keys {
        val age = stringPreferencesKey("age_band")
        val rules = booleanPreferencesKey("rules_accepted")
        val consent = intPreferencesKey("consent_version")
        val diagnostics = booleanPreferencesKey("diagnostics")
        val name = stringPreferencesKey("name")
        val seed = stringPreferencesKey("avatar_seed")
        val match = stringPreferencesKey("active_match")
    }

    val state: Flow<PlayerState> = context.playerStore.data.map { p ->
        val name = p[Keys.name]
        val seed = p[Keys.seed]
        PlayerState(
            ageBand = p[Keys.age]?.let { v -> AgeBand.entries.firstOrNull { it.name == v } },
            rulesAccepted = p[Keys.rules] ?: false,
            consentVersion = p[Keys.consent] ?: 0,
            diagnostics = p[Keys.diagnostics] ?: false,
            profile = if (name != null && seed != null) PlayerProfile(name, seed) else null,
            activeMatchId = p[Keys.match],
        )
    }

    suspend fun current(): PlayerState = state.first()

    suspend fun setAgeBand(band: AgeBand) = edit { it[Keys.age] = band.name }

    suspend fun acceptRules() = edit { it[Keys.rules] = true }

    suspend fun acceptConsent(diagnostics: Boolean) = edit {
        it[Keys.consent] = PlayerState.CURRENT_CONSENT
        it[Keys.diagnostics] = diagnostics
    }

    suspend fun setDiagnostics(enabled: Boolean) = edit { it[Keys.diagnostics] = enabled }

    suspend fun setProfile(profile: PlayerProfile) = edit {
        it[Keys.name] = profile.name
        it[Keys.seed] = profile.avatarSeed
    }

    suspend fun setActiveMatch(matchId: String?) = edit {
        if (matchId == null) it.remove(Keys.match) else it[Keys.match] = matchId
    }

    /** Account deletion: forget everything except the age band, so the gate cannot be retried. */
    suspend fun clearAccount() = edit { prefs ->
        val age = prefs[Keys.age]
        prefs.clear()
        if (age != null) prefs[Keys.age] = age
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.playerStore.edit(block)
    }
}
