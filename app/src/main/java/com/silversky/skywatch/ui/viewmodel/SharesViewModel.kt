package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.logger.Logger
import com.silversky.core.model.SmbEntry
import com.silversky.skywatch.data.remote.SmbConnectionManager
import com.silversky.skywatch.error.AppErrorEvent
import com.silversky.skywatch.error.AppErrorReporter
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

  var error by mutableStateOf<String?>(null)
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
        AppErrorReporter.report(
            AppErrorEvent.ConnectionLost(server?.name ?: server?.ipAddress ?: "server")
        )
        withContext(Dispatchers.Main) {
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
