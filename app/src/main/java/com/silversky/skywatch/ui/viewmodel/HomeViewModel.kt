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
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DialogState {
  data object None : DialogState

  data class Server(
      val editingServer: SavedServer? = null,
      val scannedAddress: String = "",
      val scannedName: String = "",
  ) : DialogState

  data class DeleteConfirmation(val server: SavedServer) : DialogState

  data object Scan : DialogState

  data class Error(val message: String) : DialogState
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

  var connectingServer by mutableStateOf<SavedServer?>(null)
    private set

  fun addServer() {
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
      connectingServer = savedServer
      connectionManager.connect(savedServer)
      connectingServer = null
      if (connectionManager.connectionState.value is ConnectionState.Connected) {
        onConnected()
      } else if (connectionManager.connectionState.value is ConnectionState.Error) {
        dialog =
            DialogState.Error(
                (connectionManager.connectionState.value as ConnectionState.Error).message
            )
      }
    }
  }

  fun confirmDelete(server: SavedServer) {
    dialog = DialogState.DeleteConfirmation(server)
  }

  fun performDelete(server: SavedServer) {
    viewModelScope.launch {
      repository.deleteServer(server.server.ipAddress)
      dismissDialog()
    }
  }
}
