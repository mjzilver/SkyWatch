package com.silversky.skywatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbServer
import com.silversky.core.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val logger: Logger
) : ViewModel() {

    private val scanner = SmbScanner()

    private val _servers =
        MutableStateFlow<List<SmbServer>>(emptyList())

    val servers: StateFlow<List<SmbServer>> =
        _servers.asStateFlow()

    private val _scanning =
        MutableStateFlow(false)

    val scanning: StateFlow<Boolean> =
        _scanning.asStateFlow()

    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()

    fun scanNetwork() {
        if (_scanning.value) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _scanning.value = true
            _error.value = null

            try {
                logger.info("Starting SMB network scan")

                val found = scanner.scanNetwork(logger)

                logger.info(
                    "SMB scan found ${found.size} servers"
                )

                found.forEach { server ->
                    logger.info(
                        "Found SMB server: " +
                                "${server.name ?: "unknown"} " +
                                "(${server.ipAddress}:${server.port})"
                    )
                }

                _servers.value = found
            } catch (e: Exception) {
                logger.error(
                    "SMB network scan failed",
                    e
                )

                _error.value =
                    e.message ?: "Network scan failed"
            } finally {
                _scanning.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}