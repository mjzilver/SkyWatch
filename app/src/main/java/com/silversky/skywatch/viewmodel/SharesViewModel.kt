package com.silversky.skywatch.viewmodel

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

  val client
    get() = connectionManager.smbClient

  val server
    get() = connectionManager.selectedServer

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
