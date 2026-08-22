package com.silversky.skywatch.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.logger.Logger
import com.silversky.skywatch.manager.ConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SharesViewModel
@Inject
constructor(
    private val connectionManager: ConnectionManager,
    private val logger: Logger,
) : ViewModel() {

  var shares by mutableStateOf<List<String>>(emptyList())
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
    val server = server ?: return

    loading = true
    error = null

    viewModelScope.launch(Dispatchers.IO) {
      try {
        logger.debug("Loading shares from ${server.ipAddress}")
        val result = client.listShares()
        logger.info("Found ${result.size} shares")
        withContext(Dispatchers.Main) {
          shares = result
          loading = false
        }
      } catch (e: Exception) {
        logger.error("Failed to list SMB shares", e)
        withContext(Dispatchers.Main) {
          error = e.message ?: "Failed to load shares"
          loading = false
        }
      }
    }
  }

  fun selectShare(share: String, onShareSelected: () -> Unit) {
    connectionManager.onShareSelected(share)
    onShareSelected()
  }

  fun disconnect(onDisconnected: () -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      connectionManager.disconnect()
      withContext(Dispatchers.Main) {
        onDisconnected()
      }
    }
  }
}
