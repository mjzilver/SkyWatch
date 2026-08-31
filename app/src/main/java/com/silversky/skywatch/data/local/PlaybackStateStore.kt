package com.silversky.skywatch.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.playbackDataStore by preferencesDataStore(name = "playback_positions")

@Serializable
data class PlaybackState(
    val position: Long,
    val duration: Long,
    val audioTrack: String? = null,
    val subtitleTrack: String? = null,
    val completed: Boolean = false,
    val subtitleOffset: Long = 0L,
)

class PlaybackStateStore(private val context: Context) {
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun get(
      ip: String,
      share: String,
      path: String,
  ): PlaybackState? {
    val key = stringPreferencesKey(key(ip, share, path))

    val jsonString = context.playbackDataStore.data.first()[key] ?: return null

    return try {
      json.decodeFromString<PlaybackState>(jsonString)
    } catch (e: Exception) {
      null
    }
  }

  suspend fun getForShare(ip: String, share: String): Map<String, PlaybackState> {
    val prefix = "state|$ip|$share|"
    val preferences = context.playbackDataStore.data.first()

    return preferences
        .asMap()
        .filterKeys { it.name.startsWith(prefix) }
        .mapNotNull { (key, value) ->
          if (value !is String) return@mapNotNull null
          val path = key.name.removePrefix(prefix)
          try {
            path to json.decodeFromString<PlaybackState>(value)
          } catch (e: Exception) {
            null
          }
        }
        .toMap()
  }

  suspend fun save(
      ip: String,
      share: String,
      path: String,
      state: PlaybackState,
  ) {
    val key = stringPreferencesKey(key(ip, share, path))

    context.playbackDataStore.edit { preferences ->
      preferences[key] = json.encodeToString(state)
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
