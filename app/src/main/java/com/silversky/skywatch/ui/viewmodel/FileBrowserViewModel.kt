package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.skywatch.data.local.PlaybackState
import com.silversky.skywatch.data.local.PlaybackStateStore
import com.silversky.skywatch.data.remote.SmbConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class FileBrowserViewModel
@Inject
constructor(
    private val connectionManager: SmbConnectionManager,
    val playbackStateStore: PlaybackStateStore,
    private val logger: Logger,
) : ViewModel() {

  var currentPath by mutableStateOf("")
    private set

  var entries by mutableStateOf<List<SmbEntry>>(emptyList())
    private set

  var resumeEntries by mutableStateOf<Map<String, PlaybackState>>(emptyMap())
    private set

  var loading by mutableStateOf(false)
    private set

  var error by mutableStateOf<String?>(null)
    private set

  val client
    get() = connectionManager.smbClient

  val server
    get() = connectionManager.selectedServer

  val shareName
    get() = connectionManager.selectedShare

  fun loadEntries() {
    val client = client ?: return
    val shareName = shareName ?: return

    loading = true
    error = null

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val result =
            client
                .list(
                    shareName = shareName,
                    path = currentPath,
                )
                .filter { !it.isHidden }

        val resumeMap =
            result
                .filter { !it.isDirectory }
                .mapNotNull { entry ->
                  val progress =
                      playbackStateStore.get(
                          ip = server?.ipAddress ?: "",
                          share = shareName,
                          path = entry.path,
                      )

                  progress?.let {
                    entry.path to it
                  }
                }
                .toMap()

        withContext(Dispatchers.Main) {
          entries = result
          resumeEntries = resumeMap
          loading = false
        }
      } catch (e: Exception) {
        logger.error("Failed to list //$shareName/$currentPath", e)
        withContext(Dispatchers.Main) {
          entries = emptyList()
          error = e.message ?: "Failed to load directory"
          loading = false
        }
      }
    }
  }

  fun selectFile(file: SmbEntry, onFileSelected: (SmbEntry) -> Unit) {
    connectionManager.onFileSelected(file)
    onFileSelected(file)
  }

  fun navigateTo(path: String) {
    currentPath = path
    loadEntries()
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
