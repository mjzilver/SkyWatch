package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.skywatch.data.remote.ConnectionState
import com.silversky.skywatch.data.remote.SmbConnectionManager
import com.silversky.skywatch.data.repository.ServerRepository
import com.silversky.skywatch.model.SavedServer
import com.silversky.skywatch.ui.component.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val connectionManager: SmbConnectionManager,
) : ViewModel() {

  var dialog by mutableStateOf<DialogState>(DialogState.None)
    private set

  val servers: StateFlow<List<SavedServer>> =
      repository.servers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

  var scanError by mutableStateOf<String?>(null)
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

  fun openScan() {
    dialog = DialogState.Scan
  }

  fun selectScannedServer(result: ScanResult) {
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

  fun deleteServer(server: SavedServer) {
    viewModelScope.launch {
      repository.deleteServer(server.server.ipAddress)
    }
  }
}
