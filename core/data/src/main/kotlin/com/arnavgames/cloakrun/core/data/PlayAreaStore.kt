package com.arnavgames.cloakrun.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arnavgames.cloakrun.core.game.area.PlayArea
import com.arnavgames.cloakrun.core.game.area.PlayAreaCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playAreaStore: DataStore<Preferences> by preferencesDataStore(name = "play_area")

/** Keeps the last saved play area on the device. Nothing here is uploaded. */
@Singleton
class PlayAreaStore @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("saved_area")

    val saved: Flow<PlayArea?> = context.playAreaStore.data.map { prefs -> prefs[key]?.let(PlayAreaCodec::decode) }

    suspend fun save(area: PlayArea) {
        context.playAreaStore.edit { it[key] = PlayAreaCodec.encode(area) }
    }
}
