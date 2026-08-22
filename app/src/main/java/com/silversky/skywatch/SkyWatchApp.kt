package com.silversky.skywatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.silversky.core.logger.Logger
import com.silversky.skywatch.ui.FileBrowserScreen
import com.silversky.skywatch.ui.HomeScreen
import com.silversky.skywatch.ui.PlayerScreen
import com.silversky.skywatch.ui.ScanDialog
import com.silversky.skywatch.ui.ServerDialog
import com.silversky.skywatch.ui.SettingsScreen
import com.silversky.skywatch.ui.ShareScreen
import com.silversky.skywatch.viewmodel.DialogState
import com.silversky.skywatch.viewmodel.FileBrowserViewModel
import com.silversky.skywatch.viewmodel.HomeViewModel
import com.silversky.skywatch.viewmodel.PlayerViewModel
import com.silversky.skywatch.viewmodel.SettingsViewModel
import com.silversky.skywatch.viewmodel.SharesViewModel

object Routes {
  const val HOME = "home"
  const val SHARES = "shares"
  const val BROWSER = "browser"
  const val PLAYER = "player"
  const val SETTINGS = "settings"
}

@Composable
fun SkyWatchApp(
    logger: Logger,
) {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = Routes.HOME) {
    composable(Routes.HOME) {
      val viewModel: HomeViewModel = hiltViewModel()
      val servers by viewModel.servers.collectAsStateWithLifecycle()

      HomeScreen(
          savedServers = servers,
          error = viewModel.scanError,
          onServerClick = { server ->
            viewModel.selectServer(server) {
              navController.navigate(Routes.SHARES)
            }
          },
          onEditServer = { server ->
            viewModel.editServer(server)
          },
          onAddServer = {
            viewModel.addServer()
          },
          onDeleteServer = { server ->
            viewModel.deleteServer(server)
          },
          onSettingsClick = {
            navController.navigate(Routes.SETTINGS)
          },
      )

      val dialog = viewModel.dialog
      if (dialog != DialogState.None) {
        when (dialog) {
          is DialogState.Server -> {
            val server = dialog.editingServer
            ServerDialog(
                onDismiss = { viewModel.dismissDialog() },
                onSave = { input ->
                  viewModel.saveServer(input, server?.server?.ipAddress)
                },
                onScan = { viewModel.scanNetwork() },
                initialAddress = server?.server?.ipAddress ?: dialog.scannedAddress,
                initialName = server?.server?.name ?: dialog.scannedName,
                initialUsername = server?.username ?: "",
                initialPassword = server?.password ?: "",
                initialIsGuest = server?.isGuest ?: false,
                isEditing = server != null,
            )
          }
          DialogState.Scan -> {
            ScanDialog(
                onDismiss = { viewModel.dismissDialog() },
                onServerSelected = { result ->
                  viewModel.selectScannedServer(result)
                },
                servers = viewModel.scanResults,
            )
          }
        }
      }
    }

    composable(Routes.SETTINGS) {
      val viewModel: SettingsViewModel = hiltViewModel()
      val address by viewModel.subtitleServerAddress.collectAsStateWithLifecycle()

      SettingsScreen(
          currentAddress = address,
          onAddressSave = { viewModel.updateSubtitleServerAddress(it) },
          onBack = { navController.popBackStack() },
      )
    }

    composable(Routes.SHARES) {
      val viewModel: SharesViewModel = hiltViewModel()
      val client = viewModel.client
      val server = viewModel.server

      if (client != null && server != null) {
        ShareScreen(
            client = client,
            server = server,
            logger = logger,
            onShareSelected = { share ->
              viewModel.selectShare(share) {
                navController.navigate(Routes.BROWSER)
              }
            },
            onBack = {
              viewModel.disconnect {
                navController.popBackStack()
              }
            },
        )
      }
    }

    composable(Routes.BROWSER) {
      val viewModel: FileBrowserViewModel = hiltViewModel()
      val client = viewModel.client
      val server = viewModel.server
      val share = viewModel.shareName

      if (client != null && server != null && share != null) {
        FileBrowserScreen(
            client = client,
            server = server,
            shareName = share,
            currentPath = viewModel.currentPath,
            logger = logger,
            playbackStateStore = viewModel.playbackStateStore,
            onPathChanged = { path ->
              viewModel.navigateTo(path)
            },
            onFileSelected = { file ->
              viewModel.selectFile(file) {
                navController.navigate(Routes.PLAYER)
              }
            },
            onBack = {
              viewModel.goBack {
                navController.popBackStack()
              }
            },
        )
      }
    }

    composable(Routes.PLAYER) {
      val viewModel: PlayerViewModel = hiltViewModel()
      val client = viewModel.client
      val share = viewModel.shareName
      val file = viewModel.file

      if (client != null && share != null && file != null) {
        PlayerScreen(
            client = client,
            shareName = share,
            file = file,
            logger = logger,
            playbackStateStore = viewModel.playbackStateStore,
            subtitleServerManager = viewModel.subtitleServerManager,
            onBack = {
              viewModel.back {
                navController.popBackStack()
              }
            },
        )
      }
    }
  }
}
