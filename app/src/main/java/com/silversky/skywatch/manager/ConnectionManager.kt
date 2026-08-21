package com.silversky.skywatch.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.di.ApplicationScope
import com.silversky.skywatch.model.SavedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ConnectionState {
  data object Disconnected : ConnectionState

  data object Connecting : ConnectionState

  data class Connected(val client: SmbClient, val server: SmbServer) : ConnectionState

  data class Error(val message: String) : ConnectionState
}

@Singleton
class ConnectionManager
@Inject
constructor(
    private val logger: Logger,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  var smbClient by mutableStateOf<SmbClient?>(null)
    private set

  var selectedServer by mutableStateOf<SmbServer?>(null)
    private set

  var selectedShare by mutableStateOf<String?>(null)
    private set

  var selectedFile by mutableStateOf<SmbEntry?>(null)
    private set

  suspend fun connect(savedServer: SavedServer) =
      withContext(Dispatchers.IO) {
        if (_connectionState.value is ConnectionState.Connecting) return@withContext

        disconnect()
        _connectionState.value = ConnectionState.Connecting

        val client = SmbClient(logger)
        try {
          client.connect(
              server = savedServer.server,
              username = savedServer.username,
              password = savedServer.password,
              isGuest = savedServer.isGuest,
          )

          smbClient = client
          selectedServer = savedServer.server
          selectedShare = null
          selectedFile = null

          _connectionState.value = ConnectionState.Connected(client, savedServer.server)
          logger.info("Successfully connected to ${savedServer.server.ipAddress}")
        } catch (e: Exception) {
          logger.error("Failed to connect to ${savedServer.server.ipAddress}", e)
          client.close()
          _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
        }
      }

  fun onShareSelected(share: String) {
    selectedShare = share
  }

  fun onFileSelected(file: SmbEntry) {
    selectedFile = file
  }

  fun clearFile() {
    selectedFile = null
  }

  fun clearShare() {
    selectedShare = null
  }

  fun disconnect() {
    val client = smbClient
    smbClient = null
    selectedServer = null
    selectedShare = null
    selectedFile = null
    _connectionState.value = ConnectionState.Disconnected

    applicationScope.launch(Dispatchers.IO) {
      try {
        client?.close()
        logger.info("SMB Client closed successfully")
      } catch (e: Exception) {
        logger.error("Error closing SMB Client", e)
      }
    }
  }
}
