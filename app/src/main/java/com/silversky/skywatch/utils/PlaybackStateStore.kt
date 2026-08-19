package com.silversky.skywatch.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.playbackDataStore by preferencesDataStore(name = "playback_positions")

data class PlaybackState(
    val position: Long,
    val duration: Long,
    val audioTrack: String? = null,
    val subtitleTrack: String? = null,
)

class PlaybackStateStore(private val context: Context) {
  private val gson = Gson()

  suspend fun get(
      ip: String,
      share: String,
      path: String,
  ): PlaybackState? {
    val key = stringPreferencesKey(key(ip, share, path))

    val json = context.playbackDataStore.data.first()[key] ?: return null

    return gson.fromJson(json, PlaybackState::class.java)
  }

  suspend fun save(
      ip: String,
      share: String,
      path: String,
      state: PlaybackState,
  ) {
    val key = stringPreferencesKey(key(ip, share, path))

    context.playbackDataStore.edit { preferences ->
      preferences[key] = gson.toJson(state)
    }
  }

  private fun key(
      ip: String,
      share: String,
      path: String,
  ): String {
    return "state|$ip|$share|$path"
  }
}
