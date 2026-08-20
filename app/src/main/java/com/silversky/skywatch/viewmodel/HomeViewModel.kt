package com.silversky.skywatch.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.manager.ConnectionManager
import com.silversky.skywatch.model.SavedServer
import com.silversky.skywatch.persistence.ServerStore
import com.silversky.skywatch.ui.ScanResult
import com.silversky.skywatch.ui.ServerConnectionInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface DialogState {
  data object None : DialogState

  data class Server(
      val editingServer: SavedServer? = null,
      val scannedAddress: String = "",
      val scannedName: String = "",
  ) : DialogState

  data object Scan : DialogState
}

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val persistenceManager: ServerStore,
    private val logger: Logger,
    private val connectionManager: ConnectionManager,
) : ViewModel() {

  var dialog by mutableStateOf<DialogState>(DialogState.None)
    private set

  var servers by mutableStateOf<List<SavedServer>>(emptyList())
    private set

  var scanResults by mutableStateOf<List<ScanResult>>(emptyList())
    private set

  var scanError by mutableStateOf<String?>(null)
    private set

  var scanning by mutableStateOf(false)
    private set

  init {
    loadServers()
  }

  private fun loadServers() {
    viewModelScope.launch(Dispatchers.IO) {
      val loadedServers = persistenceManager.getServers()
      withContext(Dispatchers.Main) {
        servers = loadedServers
      }
    }
  }

  fun addServer() {
    scanError = null
    dialog = DialogState.Server()
  }

  fun editServer(server: SavedServer) {
    dialog = DialogState.Server(editingServer = server)
  }

  fun dismissDialog() {
    dialog = DialogState.None
  }

  fun scanNetwork() {
    if (scanning) return
    dialog = DialogState.Scan
    scanning = true
    scanError = null
    scanResults = emptyList()

    viewModelScope.launch(Dispatchers.IO) {
      try {
        logger.info("Starting SMB network scan")
        val discoveredServers = SmbScanner().scanNetwork(logger)
        val results = discoveredServers.map { server ->
          ScanResult(ip = server.ipAddress, name = server.name ?: server.ipAddress)
        }
        withContext(Dispatchers.Main) {
          scanResults = results
          scanning = false
        }
      } catch (e: Exception) {
        logger.error("SMB network scan failed", e)
        withContext(Dispatchers.Main) {
          scanning = false
          scanError = e.message ?: "Network scan failed"
        }
      }
    }
  }

  fun selectScannedServer(result: ScanResult) {
    if (dialog !is DialogState.Scan) return
    dialog = DialogState.Server(scannedAddress = result.ip, scannedName = result.name)
  }

  fun connect(input: ServerConnectionInput, onConnected: () -> Unit) {
    val server = SmbServer(name = input.name, ipAddress = input.address)
    logger.info("Connecting to ${server.ipAddress}")

    viewModelScope.launch(Dispatchers.IO) {
      val client = SmbClient(logger)
      try {
        client.connect(
            server = server,
            username = input.username,
            password = input.password,
            isGuest = input.isGuest,
        )
        val saved = SavedServer(server, input.username, input.password, input.isGuest)

        val existingServers = persistenceManager.getServers()
        if (existingServers.none { it.server.ipAddress == server.ipAddress }) {
          persistenceManager.saveServer(saved)
        }
        val updatedServers = persistenceManager.getServers()

        withContext(Dispatchers.Main) {
          connectionManager.onConnected(client, server)
          servers = updatedServers
          dialog = DialogState.None
          onConnected()
        }
      } catch (e: Exception) {
        logger.error("Failed to connect to ${server.ipAddress}", e)
        client.close()
        withContext(Dispatchers.Main) {
          scanError = e.message ?: "Connection failed"
        }
      }
    }
  }

  fun selectServer(savedServer: SavedServer, onConnected: () -> Unit) {
    connect(
        ServerConnectionInput(
            name = savedServer.server.name ?: "",
            address = savedServer.server.ipAddress,
            username = savedServer.username,
            password = savedServer.password,
            isGuest = savedServer.isGuest,
        ),
        onConnected,
    )
  }

  fun updateServer(input: ServerConnectionInput, oldServer: SmbServer) {
    val server = SmbServer(name = input.name, ipAddress = input.address)
    viewModelScope.launch(Dispatchers.IO) {
      try {
        persistenceManager.updateServer(
            SavedServer(server, input.username, input.password, input.isGuest),
            oldServer,
        )
        val updatedServers = persistenceManager.getServers()
        withContext(Dispatchers.Main) {
          servers = updatedServers
          dialog = DialogState.None
        }
      } catch (e: Exception) {
        logger.error("Failed to update server", e)
      }
    }
  }

  fun deleteServer(server: SavedServer) {
    viewModelScope.launch(Dispatchers.IO) {
      persistenceManager.deleteServer(server.server)
      val updatedServers = persistenceManager.getServers()
      withContext(Dispatchers.Main) {
        servers = updatedServers
      }
    }
  }
}
