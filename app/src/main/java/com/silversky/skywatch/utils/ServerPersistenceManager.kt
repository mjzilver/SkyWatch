package com.silversky.skywatch.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.model.SavedServer
import java.io.File

class ServerPersistenceManager(private val context: Context) {
  private val gson = Gson()
  private val fileName = "saved_servers.json"
  private var serverCache: MutableList<SavedServer>? = null

  fun getServers(): List<SavedServer> {
    if (serverCache == null) {
      serverCache = loadServersFromDisk()
    }

    return serverCache?.toList() ?: emptyList()
  }

  private fun getMutableServers(): MutableList<SavedServer> {
    if (serverCache == null) {
      serverCache = loadServersFromDisk()
    }

    return serverCache!!
  }

  fun saveServer(server: SavedServer) {
    val servers = getMutableServers()

    servers.add(server)
    saveServers(servers)
  }

  fun updateServer(
      new: SavedServer,
      old: SmbServer,
  ) {
    val servers = getMutableServers()

    val index = servers.indexOfFirst {
      it.server.ipAddress == old.ipAddress
    }

    if (index != -1) {
      servers[index] = new
      saveServers(servers)
    }
  }

  fun deleteServer(server: SmbServer) {
    val servers = getMutableServers()

    servers.removeAll {
      it.server.ipAddress == server.ipAddress
    }

    saveServers(servers)
  }

  fun saveServers(servers: List<SavedServer>) {
    try {
      val json = gson.toJson(servers)
      val file = File(context.filesDir, fileName)

      file.writeText(json)

      serverCache = servers.toMutableList()

      Log.d(
          "ServerPersistence",
          "Saved ${servers.size} servers",
      )
    } catch (e: Exception) {
      Log.e(
          "ServerPersistence",
          "Failed to save servers",
          e,
      )
    }
  }

  private fun loadServersFromDisk(): MutableList<SavedServer> {
    try {
      val file = File(context.filesDir, fileName)

      if (!file.exists()) {
        return mutableListOf()
      }

      val json = file.readText()
      val type = object : TypeToken<List<SavedServer>>() {}.type
      val servers = gson.fromJson<List<SavedServer>>(json, type)

      Log.d(
          "ServerPersistence",
          "Loaded ${servers?.size ?: 0} servers",
      )

      return servers?.toMutableList() ?: mutableListOf()
    } catch (e: Exception) {
      Log.e(
          "ServerPersistence",
          "Failed to load servers",
          e,
      )

      return mutableListOf()
    }
  }
}
