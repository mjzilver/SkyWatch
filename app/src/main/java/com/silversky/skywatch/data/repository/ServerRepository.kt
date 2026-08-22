package com.silversky.skywatch.data.repository

import com.silversky.skywatch.model.SavedServer
import kotlinx.coroutines.flow.StateFlow

interface ServerRepository {
  val servers: StateFlow<List<SavedServer>>

  suspend fun addServer(server: SavedServer)

  suspend fun updateServer(oldIp: String, new: SavedServer)

  suspend fun deleteServer(ipAddress: String)

  suspend fun refresh()
}
