package com.silversky.skywatch.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.playbackDataStore by preferencesDataStore(name = "playback_positions")

class PlaybackPositionStore(private val context: Context) {

  suspend fun getPosition(
      ip: String,
      share: String,
      path: String,
  ): Long {
    val key = longPreferencesKey(key(ip, share, path))

    return context.playbackDataStore.data.first()[key] ?: 0L
  }

  suspend fun savePosition(
      ip: String,
      share: String,
      path: String,
      position: Long,
  ) {
    val key = longPreferencesKey(key(ip, share, path))

    context.playbackDataStore.edit { preferences ->
      preferences[key] = position
    }
  }

  suspend fun clearPosition(
      ip: String,
      share: String,
      path: String,
  ) {
    val key = longPreferencesKey(key(ip, share, path))

    context.playbackDataStore.edit { preferences ->
      preferences.remove(key)
    }
  }

  private fun key(
      ip: String,
      share: String,
      path: String,
  ): String {
    return "$ip|$share|$path"
  }
}
