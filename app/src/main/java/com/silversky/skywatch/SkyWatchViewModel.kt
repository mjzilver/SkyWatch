package com.silversky.skywatch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.model.SavedServer
import com.silversky.skywatch.ui.ScanResult
import com.silversky.skywatch.ui.ServerConnectionInput
import com.silversky.skywatch.utils.ServerPersistenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
  HOME,
  SHARES,
  BROWSER,
  PLAYER,
}

sealed interface DialogState {
  data object None : DialogState

  data class Server(
      val editingServer: SavedServer? = null,
      val scannedAddress: String = "",
      val scannedName: String = "",
  ) : DialogState

  data object Scan : DialogState
}

class SkyWatchViewModel(
    private val persistenceManager: ServerPersistenceManager,
    private val logger: Logger,
) : ViewModel() {

  var screen by mutableStateOf(Screen.HOME)
    private set

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

  var selectedServer by mutableStateOf<SmbServer?>(null)
    private set

  var selectedShare by mutableStateOf<String?>(null)
    private set

  var selectedFile by mutableStateOf<SmbEntry?>(null)
    private set

  var smbClient by mutableStateOf<SmbClient?>(null)
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

    dialog =
        DialogState.Server(
            scannedAddress = "",
            scannedName = "",
        )
  }

  fun editServer(server: SavedServer) {
    dialog =
        DialogState.Server(
            editingServer = server,
        )
  }

  fun dismissDialog() {
    dialog = DialogState.None
  }

  fun scanNetwork() {
    if (scanning) {
      return
    }

    dialog = DialogState.Scan
    scanning = true
    scanError = null
    scanResults = emptyList()

    viewModelScope.launch(Dispatchers.IO) {
      try {
        logger.info("Starting SMB network scan")

        val discoveredServers = SmbScanner().scanNetwork(logger)

        discoveredServers.forEach { server ->
          logger.info("Found server ${server.name} with ip ${server.ipAddress}")
        }

        val results = discoveredServers.map { server ->
          ScanResult(
              ip = server.ipAddress,
              name = server.name ?: server.ipAddress,
          )
        }

        withContext(Dispatchers.Main) {
          scanResults = results
          scanning = false
        }
      } catch (e: Exception) {
        logger.error(
            "SMB network scan failed",
            e,
        )

        withContext(Dispatchers.Main) {
          scanning = false
          scanError = e.message ?: "Network scan failed"
        }
      }
    }
  }

  fun selectScannedServer(result: ScanResult) {
    val currentDialog = dialog

    if (currentDialog !is DialogState.Scan) {
      return
    }

    dialog =
        DialogState.Server(
            scannedAddress = result.ip,
            scannedName = result.name,
        )
  }

  fun connect(input: ServerConnectionInput) {
    val server =
        SmbServer(
            name = input.name,
            ipAddress = input.address,
        )

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

        val saved =
            SavedServer(
                server = server,
                username = input.username,
                password = input.password,
                isGuest = input.isGuest,
            )

        val existingServers = persistenceManager.getServers()

        if (
            existingServers.none {
              it.server.ipAddress == server.ipAddress
            }
        ) {
          persistenceManager.saveServer(saved)
        }

        val updatedServers = persistenceManager.getServers()

        withContext(Dispatchers.Main) {
          smbClient = client
          selectedServer = server
          selectedShare = null
          selectedFile = null

          servers = updatedServers

          dialog = DialogState.None
          screen = Screen.SHARES
        }
      } catch (e: Exception) {
        logger.error(
            "Failed to connect to ${server.ipAddress}",
            e,
        )

        client.close()

        withContext(Dispatchers.Main) {
          scanError = e.message ?: "Connection failed"
        }
      }
    }
  }

  fun selectServer(savedServer: SavedServer) {
    connect(
        ServerConnectionInput(
            name = savedServer.server.name ?: "",
            address = savedServer.server.ipAddress,
            username = savedServer.username,
            password = savedServer.password,
            isGuest = savedServer.isGuest,
        )
    )
  }

  fun updateServer(
      input: ServerConnectionInput,
      oldServer: SmbServer,
  ) {
    val server =
        SmbServer(
            name = input.name,
            ipAddress = input.address,
        )

    logger.info("Updating server ${server.ipAddress}")

    viewModelScope.launch(Dispatchers.IO) {
      try {
        persistenceManager.updateServer(
            SavedServer(
                server = server,
                username = input.username,
                password = input.password,
                isGuest = input.isGuest,
            ),
            oldServer,
        )

        val updatedServers = persistenceManager.getServers()

        withContext(Dispatchers.Main) {
          servers = updatedServers
          dialog = DialogState.None
        }
      } catch (e: Exception) {
        logger.error(
            "Failed to update server",
            e,
        )
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

  fun selectShare(share: String) {
    selectedShare = share
    screen = Screen.BROWSER
  }

  fun selectFile(file: SmbEntry) {
    selectedFile = file
    screen = Screen.PLAYER
  }

  fun back() {
    when {
      dialog is DialogState.Scan -> {
        dialog = DialogState.Server()
      }

      dialog is DialogState.Server -> {
        dialog = DialogState.None
      }

      screen == Screen.PLAYER -> {
        selectedFile = null
        screen = Screen.BROWSER
      }

      screen == Screen.BROWSER -> {
        selectedShare = null
        screen = Screen.SHARES
      }

      screen == Screen.SHARES -> {
        disconnect()
      }

      screen == Screen.HOME -> Unit
    }
  }

  fun disconnect() {
    val client = smbClient

    smbClient = null
    selectedServer = null
    selectedShare = null
    selectedFile = null
    screen = Screen.HOME

    viewModelScope.launch(Dispatchers.IO) {
      client?.close()
    }
  }

  override fun onCleared() {
    val client = smbClient

    smbClient = null

    viewModelScope.launch(Dispatchers.IO) {
      client?.close()
    }
  }
}
