package com.silversky.skywatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.silversky.skywatch.ui.component.ScanDialog
import com.silversky.skywatch.ui.component.ServerDialog
import com.silversky.skywatch.ui.component.SkyWatchDialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.silversky.skywatch.ui.screen.FileBrowserScreen
import com.silversky.skywatch.ui.screen.HomeScreen
import com.silversky.skywatch.ui.screen.PlayerScreen
import com.silversky.skywatch.ui.screen.SeriesDetailScreen
import com.silversky.skywatch.ui.screen.SettingsScreen
import com.silversky.skywatch.ui.screen.ShareScreen
import com.silversky.skywatch.ui.viewmodel.DialogState
import com.silversky.skywatch.ui.viewmodel.FileBrowserViewModel
import com.silversky.skywatch.ui.viewmodel.HomeViewModel
import com.silversky.skywatch.ui.viewmodel.PlayerViewModel
import com.silversky.skywatch.ui.viewmodel.ScanViewModel
import com.silversky.skywatch.ui.viewmodel.SeriesDetailViewModel
import com.silversky.skywatch.ui.viewmodel.ServerViewModel
import com.silversky.skywatch.ui.viewmodel.SettingsViewModel
import com.silversky.skywatch.ui.viewmodel.SharesViewModel

object Routes {
  const val HOME = "home"
  const val SHARES = "shares"
  const val BROWSER = "browser"
  const val SERIES_DETAIL = "series_detail"
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
            val serverViewModel: ServerViewModel = hiltViewModel()
            val server = dialog.editingServer
            ServerDialog(
                onDismiss = { viewModel.dismissDialog() },
                onSave = { input ->
                  serverViewModel.saveServer(input, server?.server?.ipAddress) {
                    viewModel.dismissDialog()
                  }
                },
                onScan = { viewModel.openScan() },
                initialAddress = server?.server?.ipAddress ?: dialog.scannedAddress,
                initialName = server?.server?.name ?: dialog.scannedName,
                initialUsername = server?.username ?: "",
                initialPassword = server?.password ?: "",
                initialIsGuest = server?.isGuest ?: false,
                isEditing = server != null,
            )
          }
          is DialogState.DeleteConfirmation -> {
            SkyWatchDialog(
                title = "Delete Server",
                onDismiss = { viewModel.dismissDialog() },
                buttons = {
                  Button(onClick = { viewModel.performDelete(dialog.server) }) {
                    Text("Delete")
                  }
                  Button(onClick = { viewModel.dismissDialog() }) {
                    Text("Cancel")
                  }
                }
            ) {
              Text(
                  text = "Are you sure you want to delete '${dialog.server.server.name ?: dialog.server.server.ipAddress}'?",
                  style = MaterialTheme.typography.bodyLarge,
                  color = Color.White,
              )
            }
          }
          DialogState.Scan -> {
            val scanViewModel: ScanViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
              scanViewModel.scanNetwork()
            }
            ScanDialog(
                onDismiss = { viewModel.dismissDialog() },
                onServerSelected = { result ->
                  viewModel.selectScannedServer(result)
                },
                servers = scanViewModel.scanResults,
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
          onShareSelected = {
            navController.navigate(Routes.BROWSER)
          },
          onBack = {
            viewModel.disconnect {
              if (navController.currentBackStackEntry?.destination?.route == Routes.SHARES) {
                navController.popBackStack()
              }
            }
          },
      )
    }

    composable(Routes.BROWSER) {
      val viewModel: FileBrowserViewModel = hiltViewModel()

      FileBrowserScreen(
          viewModel = viewModel,
          onFileSelected = { navController.navigate(Routes.PLAYER) },
          onSeriesSelected = { navController.navigate(Routes.SERIES_DETAIL) },
          onBack = { navController.popBackStack() },
      )
    }

    composable(Routes.SERIES_DETAIL) {
      val viewModel: SeriesDetailViewModel = hiltViewModel()
      SeriesDetailScreen(
          viewModel = viewModel,
          onEpisodeSelected = { navController.navigate(Routes.PLAYER) },
          onBack = { navController.popBackStack() },
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
