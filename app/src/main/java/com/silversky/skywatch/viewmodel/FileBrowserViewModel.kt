package com.silversky.skywatch.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.skywatch.manager.ConnectionManager
import com.silversky.skywatch.persistence.PlaybackStateStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FileBrowserViewModel
@Inject
constructor(
    private val connectionManager: ConnectionManager,
    val playbackStateStore: PlaybackStateStore,
    private val logger: Logger,
) : ViewModel() {

  var currentPath by mutableStateOf("")
    private set

  val client
    get() = connectionManager.smbClient

  val server
    get() = connectionManager.selectedServer

  val shareName
    get() = connectionManager.selectedShare

  fun selectFile(file: SmbEntry, onFileSelected: () -> Unit) {
    connectionManager.onFileSelected(file)
    onFileSelected()
  }

  fun navigateTo(path: String) {
    currentPath = path
  }

  fun goBack(onBack: () -> Unit) {
    if (currentPath.isEmpty()) {
      connectionManager.clearShare()
      onBack()
    } else {
      currentPath = parentPath(currentPath)
    }
  }

  private fun parentPath(path: String): String {
    val normalized = path.trimEnd('\\')
    val index = normalized.lastIndexOf('\\')
    return if (index < 0) "" else normalized.substring(0, index)
  }
}
