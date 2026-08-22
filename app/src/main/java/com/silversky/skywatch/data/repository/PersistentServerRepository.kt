package com.silversky.skywatch.data.repository

import android.content.Context
import android.util.Log
import com.silversky.skywatch.model.SavedServer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class PersistentServerRepository
@Inject
constructor(
    private val context: Context,
) : ServerRepository {

  private val json = Json { ignoreUnknownKeys = true }
  private val fileName = "saved_servers.json"

  private val _servers = MutableStateFlow<List<SavedServer>>(emptyList())
  override val servers: StateFlow<List<SavedServer>> = _servers.asStateFlow()

  init {
    loadServersFromDisk()
  }

  override suspend fun addServer(server: SavedServer) =
      withContext(Dispatchers.IO) {
        val current = _servers.value.toMutableList()
        current.add(server)
        saveServersToDisk(current)
      }

  override suspend fun updateServer(oldIp: String, new: SavedServer) =
      withContext(Dispatchers.IO) {
        val current = _servers.value.toMutableList()
        val index = current.indexOfFirst { it.server.ipAddress == oldIp }
        if (index != -1) {
          current[index] = new
          saveServersToDisk(current)
        }
      }

  override suspend fun deleteServer(ipAddress: String) =
      withContext(Dispatchers.IO) {
        val current = _servers.value.toMutableList()
        current.removeAll { it.server.ipAddress == ipAddress }
        saveServersToDisk(current)
      }

  override suspend fun refresh() {
    withContext(Dispatchers.IO) {
      loadServersFromDisk()
    }
  }

  private fun loadServersFromDisk() {
    try {
      val file = File(context.filesDir, fileName)
      if (!file.exists()) {
        _servers.value = emptyList()
        return
      }

      val jsonString = file.readText()
      val loadedServers = json.decodeFromString<List<SavedServer>>(jsonString)

      _servers.value = loadedServers
      Log.d("ServerRepository", "Loaded ${loadedServers.size} servers")
    } catch (e: Exception) {
      Log.e("ServerRepository", "Failed to load servers", e)
      _servers.value = emptyList()
    }
  }

  private fun saveServersToDisk(serversToSave: List<SavedServer>) {
    try {
      val jsonString = json.encodeToString(serversToSave)
      val file = File(context.filesDir, fileName)
      file.writeText(jsonString)

      _servers.value = serversToSave
      Log.d("ServerRepository", "Saved ${serversToSave.size} servers")
    } catch (e: Exception) {
      Log.e("ServerRepository", "Failed to save servers", e)
    }
  }
}
