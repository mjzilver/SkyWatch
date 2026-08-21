package com.silversky.skywatch.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.manager.ConnectionManager
import com.silversky.skywatch.manager.ConnectionState
import com.silversky.skywatch.model.SavedServer
import com.silversky.skywatch.repository.ServerRepository
import com.silversky.skywatch.ui.ScanResult
import com.silversky.skywatch.ui.ServerConnectionInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val repository: ServerRepository,
    private val logger: Logger,
    private val connectionManager: ConnectionManager,
    private val smbScanner: SmbScanner,
) : ViewModel() {

  var dialog by mutableStateOf<DialogState>(DialogState.None)
    private set

  val servers: StateFlow<List<SavedServer>> =
      repository.servers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

  var scanResults by mutableStateOf<List<ScanResult>>(emptyList())
    private set

  var scanError by mutableStateOf<String?>(null)
    private set

  var scanning by mutableStateOf(false)
    private set

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
        val discoveredServers = smbScanner.scanNetwork(logger)
        val results = discoveredServers.map { server ->
          ScanResult(ip = server.ipAddress, name = server.name ?: server.ipAddress)
        }
        launch(Dispatchers.Main) {
          scanResults = results
          scanning = false
        }
      } catch (e: Exception) {
        logger.error("SMB network scan failed", e)
        launch(Dispatchers.Main) {
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

  fun selectServer(savedServer: SavedServer, onConnected: () -> Unit) {
    viewModelScope.launch {
      connectionManager.connect(savedServer)
      if (connectionManager.connectionState.value is ConnectionState.Connected) {
        onConnected()
      } else if (connectionManager.connectionState.value is ConnectionState.Error) {
        scanError = (connectionManager.connectionState.value as ConnectionState.Error).message
      }
    }
  }

  fun saveServer(input: ServerConnectionInput, oldIpAddress: String? = null) {
    val server = SmbServer(name = input.name, ipAddress = input.address)
    val saved = SavedServer(server, input.username, input.password, input.isGuest)

    viewModelScope.launch {
      try {
        if (oldIpAddress != null) {
          repository.updateServer(oldIpAddress, saved)
        } else {
          repository.addServer(saved)
        }
        dialog = DialogState.None
      } catch (e: Exception) {
        logger.error("Failed to save server", e)
      }
    }
  }

  fun deleteServer(server: SavedServer) {
    viewModelScope.launch {
      repository.deleteServer(server.server.ipAddress)
    }
  }
}
