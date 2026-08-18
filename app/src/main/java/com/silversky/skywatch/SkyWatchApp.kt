package com.silversky.skywatch

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.model.SavedServer
import com.silversky.skywatch.ui.FileBrowserScreen
import com.silversky.skywatch.ui.HomeScreen
import com.silversky.skywatch.ui.PlayerScreen
import com.silversky.skywatch.ui.ScanDialog
import com.silversky.skywatch.ui.ScanResult
import com.silversky.skywatch.ui.ServerConnectionInput
import com.silversky.skywatch.ui.ServerDialog
import com.silversky.skywatch.ui.ShareScreen
import com.silversky.skywatch.utils.ServerPersistenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen {
  HOME,
  SHARES,
  BROWSER,
  PLAYER,
}

@Composable
fun SkyWatchApp(
    logger: Logger,
    context: Context,
) {
  val scope = rememberCoroutineScope()

  val persistenceManager = remember {
    ServerPersistenceManager(context)
  }

  val prefs = remember {
    context.getSharedPreferences(
        "skywatch_prefs",
        Context.MODE_PRIVATE,
    )
  }

  var screen by remember {
    mutableStateOf(Screen.HOME)
  }

  var showServerDialog by remember {
    mutableStateOf(false)
  }

  var showScanDialog by remember {
    mutableStateOf(false)
  }

  var editingServer by remember {
    mutableStateOf<SavedServer?>(null)
  }

  var cachedServers by remember {
    mutableStateOf<List<SavedServer>>(emptyList())
  }

  var scanResults by remember {
    mutableStateOf<List<ScanResult>>(emptyList())
  }

  var scanError by remember {
    mutableStateOf<String?>(null)
  }

  var scanning by remember {
    mutableStateOf(false)
  }

  var scannedAddress by remember {
    mutableStateOf("")
  }

  var scannedName by remember {
    mutableStateOf("")
  }

  var selectedServer by remember {
    mutableStateOf<SmbServer?>(null)
  }

  var selectedShare by remember {
    mutableStateOf<String?>(null)
  }

  var selectedFile by remember {
    mutableStateOf<SmbEntry?>(null)
  }

  var smbClient by remember {
    mutableStateOf<SmbClient?>(null)
  }

  LaunchedEffect(Unit) {
    cachedServers =
        withContext(Dispatchers.IO) {
          persistenceManager.getServers()
        }
  }

  fun connect(input: ServerConnectionInput) {
    val server =
        SmbServer(
            name = input.name,
            ipAddress = input.address,
        )

    logger.info("Connecting to ${server.ipAddress}")

    scope.launch(Dispatchers.IO) {
      val client = SmbClient(logger)

      try {
        client.connect(
            server = server,
            username = input.username,
            password = input.password,
        )

        val saved =
            SavedServer(
                server = server,
                username = input.username,
                password = input.password,
            )

        val existingServers = persistenceManager.getServers()

        if (
            existingServers.none {
              it.server.ipAddress == server.ipAddress
            }
        ) {
          persistenceManager.saveServer(saved)
        }

        val servers = persistenceManager.getServers()

        withContext(Dispatchers.Main) {
          smbClient = client
          selectedServer = server
          selectedShare = null
          selectedFile = null

          cachedServers = servers

          showServerDialog = false
          showScanDialog = false
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
            name = savedServer.server.name,
            address = savedServer.server.ipAddress,
            username = savedServer.username,
            password = savedServer.password,
        )
    )
  }

  fun scanNetwork() {
    if (scanning) {
      return
    }

    showServerDialog = false
    showScanDialog = true

    scanning = true
    scanError = null
    scanResults = emptyList()

    scope.launch(Dispatchers.IO) {
      try {
        logger.info("Starting SMB network scan")

        val discoveredServers = SmbScanner().scanNetwork(logger)

        discoveredServers.forEach { server ->
          logger.info("Found server ${server.name} " + "with ip ${server.ipAddress}")
        }

        val savedServers = persistenceManager.getServers().map { it.server }
        val savedIps = savedServers.map { it.ipAddress }.toSet()
        val newServers =
            discoveredServers.distinctBy { it.ipAddress }.filter { it.ipAddress !in savedIps }

        logger.info("Found ${newServers.size} new SMB servers")

        val results = newServers.map { server ->
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

    scope.launch(Dispatchers.IO) {
      try {
        persistenceManager.updateServer(
            SavedServer(
                server = server,
                username = input.username,
                password = input.password,
            ),
            oldServer,
        )

        val servers = persistenceManager.getServers()

        withContext(Dispatchers.Main) {
          cachedServers = servers
        }
      } catch (e: Exception) {
        logger.error(
            "Failed to update server",
            e,
        )
      }
    }
  }

  BackHandler(enabled = screen != Screen.HOME || showServerDialog || showScanDialog) {
    when {
      showScanDialog -> {
        showScanDialog = false
        showServerDialog = true
      }

      showServerDialog -> {
        showServerDialog = false
      }

      else -> {
        when (screen) {
          Screen.PLAYER -> {
            selectedFile = null
            screen = Screen.BROWSER
          }

          Screen.BROWSER -> {
            selectedShare = null
            screen = Screen.SHARES
          }

          Screen.SHARES -> {
            scope.launch(Dispatchers.IO) {
              smbClient?.close()
            }

            smbClient = null
            selectedServer = null
            selectedShare = null
            selectedFile = null
            screen = Screen.HOME
          }

          Screen.HOME -> Unit
        }
      }
    }
  }

  when (screen) {
    Screen.HOME -> {
      HomeScreen(
          savedServers = cachedServers,
          error = scanError,
          onServerClick = { savedServer ->
            logger.info("Saved server clicked")

            selectServer(savedServer)
          },
          onEditServer = { savedServer ->
            logger.info("Edit server clicked: " + savedServer.server.ipAddress)

            editingServer = savedServer
            showServerDialog = true
          },
          onAddServer = {
            editingServer = null
            scannedAddress = ""
            scannedName = ""
            scanError = null
            showServerDialog = true
          },
          onDeleteServer = { savedServer ->
            scope.launch(Dispatchers.IO) {
              persistenceManager.deleteServer(savedServer.server)

              val servers = persistenceManager.getServers()

              withContext(Dispatchers.Main) {
                cachedServers = servers
              }
            }
          },
      )
    }

    Screen.SHARES -> {
      val client = smbClient
      val server = selectedServer

      if (client != null && server != null) {
        ShareScreen(
            client = client,
            server = server,
            logger = logger,
            onShareSelected = { share ->
              selectedShare = share
              screen = Screen.BROWSER
            },
            onBack = {
              scope.launch(Dispatchers.IO) {
                client.close()
              }

              smbClient = null
              selectedServer = null
              selectedShare = null
              selectedFile = null
              screen = Screen.HOME
            },
        )
      }
    }

    Screen.BROWSER -> {
      val client = smbClient
      val server = selectedServer
      val share = selectedShare

      if (client != null && server != null && share != null) {
        FileBrowserScreen(
            client = client,
            server = server,
            shareName = share,
            logger = logger,
            onFileSelected = { file ->
              selectedFile = file
              screen = Screen.PLAYER
            },
            onBack = {
              selectedShare = null
              screen = Screen.SHARES
            },
        )
      }
    }

    Screen.PLAYER -> {
      val client = smbClient
      val share = selectedShare
      val file = selectedFile

      if (client != null && share != null && file != null) {
        PlayerScreen(
            client = client,
            shareName = share,
            file = file,
            logger = logger,
            onBack = {
              selectedFile = null
              screen = Screen.BROWSER
            },
        )
      }
    }
  }

  if (showServerDialog) {
    val server = editingServer

    ServerDialog(
        onDismiss = {
          showServerDialog = false
          editingServer = null
        },
        onConnect = { input ->
          if (server != null) {
            updateServer(
                input = input,
                oldServer = server.server,
            )
          } else {
            connect(input)
          }

          showServerDialog = false
          editingServer = null
        },
        onScan = ::scanNetwork,
        initialAddress = server?.server?.ipAddress ?: scannedAddress,
        initialName = server?.server?.name ?: scannedName,
        initialUsername = server?.username ?: "",
        initialPassword = server?.password ?: "",
        isEditing = server != null,
    )
  }

  if (showScanDialog) {
    ScanDialog(
        onDismiss = {
          showScanDialog = false
          showServerDialog = true
        },
        onServerSelected = { result ->
          scannedAddress = result.ip
          scannedName = result.name
          showScanDialog = false
          showServerDialog = true
        },
        servers = scanResults,
    )
  }
}
