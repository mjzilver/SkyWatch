package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbScanner
import com.silversky.skywatch.ui.component.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class ScanViewModel
@Inject
constructor(
    private val smbScanner: SmbScanner,
    private val logger: Logger,
) : ViewModel() {

  var scanResults by mutableStateOf<List<ScanResult>>(emptyList())
    private set

  var scanError by mutableStateOf<String?>(null)
    private set

  var scanning by mutableStateOf(false)
    private set

  fun scanNetwork() {
    if (scanning) return
    scanning = true
    scanError = null
    scanResults = emptyList()

    viewModelScope.launch(Dispatchers.IO) {
      try {
        logger.info("Starting SMB network scan")
        val discoveredServers = smbScanner.scanNetwork(logger)
        val results = discoveredServers.map { server ->
          ScanResult(ip = server.ipAddress, name = server.name ?: server.ipAddress)
        }
        launch(Dispatchers.Main) {
          scanResults = results
          scanning = false
        }
      } catch (e: Exception) {
        logger.error("SMB network scan failed", e)
        launch(Dispatchers.Main) {
          scanning = false
          scanError = e.message ?: "Network scan failed"
        }
      }
    }
  }
}
