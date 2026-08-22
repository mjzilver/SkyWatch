package com.silversky.skywatch

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
fun SkyWatchApp() {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = Routes.HOME) {
    composable(Routes.HOME) {
      val viewModel: HomeViewModel = hiltViewModel()

      HomeScreen(
          viewModel = viewModel,
          onServerConnected = {
            navController.navigate(Routes.SHARES)
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

      SettingsScreen(
          viewModel = viewModel,
          onBack = { navController.popBackStack() },
      )
    }

    composable(Routes.SHARES) {
      val viewModel: SharesViewModel = hiltViewModel()

      ShareScreen(
          viewModel = viewModel,
          onShareSelected = { share ->
            navController.navigate(Routes.BROWSER)
          },
          onBack = {
            viewModel.disconnect {
              navController.popBackStack()
            }
          },
      )
    }

    composable(Routes.BROWSER) {
      val viewModel: FileBrowserViewModel = hiltViewModel()

      FileBrowserScreen(
          viewModel = viewModel,
          onFileSelected = { file ->
            navController.navigate(Routes.PLAYER)
          },
          onBack = {
            navController.popBackStack()
          },
      )
    }

    composable(Routes.PLAYER) {
      val viewModel: PlayerViewModel = hiltViewModel()

      PlayerScreen(
          viewModel = viewModel,
          onBack = {
            navController.popBackStack()
          },
      )
    }
  }
}
