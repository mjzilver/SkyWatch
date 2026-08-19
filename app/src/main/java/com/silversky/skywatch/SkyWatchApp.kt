package com.silversky.skywatch

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.silversky.core.logger.Logger
import com.silversky.skywatch.ui.FileBrowserScreen
import com.silversky.skywatch.ui.HomeScreen
import com.silversky.skywatch.ui.PlayerScreen
import com.silversky.skywatch.ui.ScanDialog
import com.silversky.skywatch.ui.ServerDialog
import com.silversky.skywatch.ui.ShareScreen
import com.silversky.skywatch.utils.PlaybackStateStore
import com.silversky.skywatch.utils.ServerPersistenceManager

@Composable
fun SkyWatchApp(
    logger: Logger,
    context: Context,
) {
  val persistenceManager = remember {
    ServerPersistenceManager(context)
  }

  val playbackStateStore = remember {
    PlaybackStateStore(context)
  }

  val viewModel = remember {
    SkyWatchViewModel(
        persistenceManager = persistenceManager,
        logger = logger,
    )
  }

  BackHandler(
      enabled = viewModel.screen != Screen.HOME || viewModel.dialog != DialogState.None,
  ) {
    viewModel.back()
  }

  when (viewModel.screen) {
    Screen.HOME -> {
      HomeScreen(
          savedServers = viewModel.servers,
          error = viewModel.scanError,
          onServerClick = { server ->
            logger.info("Saved server clicked")
            viewModel.selectServer(server)
          },
          onEditServer = { server ->
            logger.info("Edit server clicked: ${server.server.ipAddress}")
            viewModel.editServer(server)
          },
          onAddServer = {
            viewModel.addServer()
          },
          onDeleteServer = { server ->
            viewModel.deleteServer(server)
          },
      )
    }

    Screen.SHARES -> {
      val client = viewModel.smbClient
      val server = viewModel.selectedServer

      if (client != null && server != null) {
        ShareScreen(
            client = client,
            server = server,
            logger = logger,
            onShareSelected = { share ->
              viewModel.selectShare(share)
            },
            onBack = {
              viewModel.disconnect()
            },
        )
      }
    }

    Screen.BROWSER -> {
      val client = viewModel.smbClient
      val server = viewModel.selectedServer
      val share = viewModel.selectedShare

      if (client != null && server != null && share != null) {
        FileBrowserScreen(
            client = client,
            server = server,
            shareName = share,
            logger = logger,
            playbackStateStore = playbackStateStore,
            onFileSelected = { file ->
              viewModel.selectFile(file)
            },
            onBack = {
              viewModel.back()
            },
        )
      }
    }

    Screen.PLAYER -> {
      val client = viewModel.smbClient
      val share = viewModel.selectedShare
      val file = viewModel.selectedFile

      if (client != null && share != null && file != null) {
        PlayerScreen(
            client = client,
            shareName = share,
            file = file,
            logger = logger,
            playbackStateStore = playbackStateStore,
            onBack = {
              viewModel.back()
            },
        )
      }
    }
  }

  when (val dialog = viewModel.dialog) {
    DialogState.None -> Unit

    is DialogState.Server -> {
      val server = dialog.editingServer

      ServerDialog(
          onDismiss = {
            viewModel.dismissDialog()
          },
          onConnect = { input ->
            if (server != null) {
              viewModel.updateServer(
                  input = input,
                  oldServer = server.server,
              )
            } else {
              viewModel.connect(input)
            }
          },
          onScan = {
            viewModel.scanNetwork()
          },
          initialAddress = server?.server?.ipAddress ?: dialog.scannedAddress,
          initialName = server?.server?.name ?: dialog.scannedName ?: "",
          initialUsername = server?.username ?: "",
          initialPassword = server?.password ?: "",
          initialIsGuest = server?.isGuest ?: false,
          isEditing = server != null,
      )
    }

    DialogState.Scan -> {
      ScanDialog(
          onDismiss = {
            viewModel.back()
          },
          onServerSelected = { result ->
            viewModel.selectScannedServer(result)
          },
          servers = viewModel.scanResults,
      )
    }
  }
}
