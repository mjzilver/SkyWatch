package com.silversky.skywatch.settings

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val fileName = "settings.json"

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val content = file.readText()
                _settings.value = json.decodeFromString(content)
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Failed to load settings", e)
        }
    }

    suspend fun updateSettings(update: (Settings) -> Settings) = withContext(Dispatchers.IO) {
        val newSettings = update(_settings.value)
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    private fun saveSettings(settings: Settings) {
        try {
            val file = File(context.filesDir, fileName)
            file.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            Log.e("SettingsManager", "Failed to save settings", e)
        }
    }
}
