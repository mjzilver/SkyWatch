package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.logger.Logger
import com.silversky.core.model.SmbServer
import com.silversky.skywatch.data.repository.ServerRepository
import com.silversky.skywatch.model.SavedServer
import com.silversky.skywatch.ui.component.ServerConnectionInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ServerViewModel
@Inject
constructor(
    private val repository: ServerRepository,
    private val logger: Logger,
) : ViewModel() {

  var isSaving by mutableStateOf(false)
    private set

  var error by mutableStateOf<String?>(null)
    private set

  fun saveServer(
      input: ServerConnectionInput,
      oldIpAddress: String? = null,
      onSuccess: () -> Unit,
  ) {
    if (isSaving) return

    val server = SmbServer(name = input.name, ipAddress = input.address)
    val saved = SavedServer(server, input.username, input.password, input.isGuest)

    viewModelScope.launch {
      isSaving = true
      error = null
      try {
        if (oldIpAddress != null) {
          repository.updateServer(oldIpAddress, saved)
        } else {
          repository.addServer(saved)
        }
        onSuccess()
      } catch (e: Exception) {
        logger.error("Failed to save server", e)
        error = e.message ?: "Failed to save server"
      } finally {
        isSaving = false
      }
    }
  }
}
