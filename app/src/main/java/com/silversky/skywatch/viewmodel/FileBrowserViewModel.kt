package com.silversky.skywatch.viewmodel

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

  fun back(onBack: () -> Unit) {
    connectionManager.clearShare()
    onBack()
  }
}
