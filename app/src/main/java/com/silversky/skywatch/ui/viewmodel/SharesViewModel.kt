package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.logger.Logger
import com.silversky.core.model.SmbEntry
import com.silversky.skywatch.data.remote.SmbConnectionManager
import com.silversky.skywatch.error.AppError
import com.silversky.skywatch.error.AppErrorEvent
import com.silversky.skywatch.error.AppErrorReporter
import com.silversky.skywatch.error.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SharesViewModel
@Inject
constructor(
    private val connectionManager: SmbConnectionManager,
    private val logger: Logger,
) : ViewModel() {

  var shares by mutableStateOf<List<SmbEntry>>(emptyList())
    private set

  var loading by mutableStateOf(false)
    private set

  var error by mutableStateOf<AppError?>(null)
    private set

  val client
    get() = connectionManager.smbClient

  val server
    get() = connectionManager.selectedServer

  fun loadShares() {
    val client = client ?: return
    server ?: return

    loading = true
    error = null

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val result = client.listShares()
        withContext(Dispatchers.Main) {
          shares = result
          loading = false
        }
      } catch (e: Exception) {
        logger.error("Failed to list SMB shares", e)
        val appError = e.toAppError("Could not connect to the server or list shares.")
        AppErrorReporter.report(
            AppErrorEvent.ConnectionLost(
                serverName = server?.name ?: server?.ipAddress ?: "server",
                technicalDetails = appError.technicalMessage,
            )
        )
        withContext(Dispatchers.Main) {
          error = appError
          loading = false
        }
      }
    }
  }

  fun selectShare(share: SmbEntry, onShareSelected: () -> Unit) {
    connectionManager.onShareSelected(share)
    onShareSelected()
  }

  fun disconnect(onDisconnected: () -> Unit) {
    viewModelScope.launch {
      connectionManager.disconnect()
      onDisconnected()
    }
  }
}
