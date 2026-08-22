package com.silversky.skywatch.viewmodel

import androidx.lifecycle.ViewModel
import com.silversky.core.logger.Logger
import com.silversky.skywatch.manager.ConnectionManager
import com.silversky.skywatch.persistence.PlaybackStateStore
import com.silversky.skywatch.subtitle.SubtitleServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
    private val connectionManager: ConnectionManager,
    val playbackStateStore: PlaybackStateStore,
    val subtitleServerManager: SubtitleServerManager,
    private val logger: Logger,
) : ViewModel() {

  val client
    get() = connectionManager.smbClient

  val shareName
    get() = connectionManager.selectedShare

  val file
    get() = connectionManager.selectedFile

  fun back(onBack: () -> Unit) {
    connectionManager.clearFile()
    onBack()
  }
}
