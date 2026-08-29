package com.silversky.skywatch.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.silversky.core.logger.Logger
import com.silversky.core.model.SmbEntry
import com.silversky.core.model.SmbServer
import com.silversky.core.smb.SmbClient
import com.silversky.skywatch.model.SavedServer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed interface ConnectionState {
  data object Disconnected : ConnectionState

  data object Connecting : ConnectionState

  data class Connected(val client: SmbClient, val server: SmbServer) : ConnectionState

  data class Error(val message: String) : ConnectionState
}

@Singleton
class SmbConnectionManager
@Inject
constructor(
    private val logger: Logger,
) {
  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  var smbClient by mutableStateOf<SmbClient?>(null)
    private set

  var selectedServer by mutableStateOf<SmbServer?>(null)
    private set

  var selectedShare by mutableStateOf<SmbEntry?>(null)
    private set

  var selectedFile by mutableStateOf<SmbEntry?>(null)
    private set

  var selectedSeriesTitle by mutableStateOf<String?>(null)
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
        } catch (e: Exception) {
          logger.error("Failed to connect to ${savedServer.server.ipAddress}", e)
          client.close()
          _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
        }
      }

  fun onShareSelected(share: SmbEntry) {
    selectedShare = share
  }

  fun onFileSelected(file: SmbEntry) {
    selectedFile = file
  }

  fun selectFileByPath(path: String, name: String) {
    val share = selectedShare?.shareName ?: return
    selectedFile =
        SmbEntry(
            name = name,
            path = path,
            type = com.silversky.core.model.SmbEntryType.File,
            shareName = share,
        )
  }

  fun onSeriesSelected(title: String) {
    selectedSeriesTitle = title
  }

  fun clearFile() {
    selectedFile = null
  }

  fun clearShare() {
    selectedShare = null
  }

  suspend fun disconnect() {
    val client = smbClient
    smbClient = null
    selectedServer = null
    selectedShare = null
    selectedFile = null
    _connectionState.value = ConnectionState.Disconnected

    withContext(Dispatchers.IO) {
      try {
        client?.close()
      } catch (e: Exception) {
        logger.error("Error closing SMB Client", e)
      }
    }
  }
}
