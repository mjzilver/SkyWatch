package com.silversky.skywatch.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionManager
@Inject
constructor(
    private val logger: Logger,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
  var smbClient by mutableStateOf<SmbClient?>(null)
    private set

  var selectedServer by mutableStateOf<SmbServer?>(null)
    private set

  var selectedShare by mutableStateOf<String?>(null)
    private set

  var selectedFile by mutableStateOf<SmbEntry?>(null)
    private set

  fun onConnected(client: SmbClient, server: SmbServer) {
    smbClient = client
    selectedServer = server
    selectedShare = null
    selectedFile = null
  }

  fun onShareSelected(share: String) {
    selectedShare = share
  }

  fun onFileSelected(file: SmbEntry) {
    selectedFile = file
  }

  fun clearFile() {
    selectedFile = null
  }

  fun clearShare() {
    selectedShare = null
  }

  fun disconnect() {
    val client = smbClient
    smbClient = null
    selectedServer = null
    selectedShare = null
    selectedFile = null

    applicationScope.launch(Dispatchers.IO) {
      try {
        client?.close()
        logger.info("SMB Client closed successfully during disconnect")
      } catch (e: Exception) {
        logger.error("Error closing SMB Client during disconnect", e)
      }
    }
  }
}
