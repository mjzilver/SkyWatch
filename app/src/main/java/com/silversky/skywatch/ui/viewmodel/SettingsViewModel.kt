package com.silversky.skywatch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.skywatch.data.remote.SubtitleServerDiscovery
import com.silversky.skywatch.data.repository.SettingsRepository
import com.silversky.skywatch.data.repository.SubtitleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

sealed interface SubtitleServerConnectionStatus {
  data object Idle : SubtitleServerConnectionStatus

  data object Testing : SubtitleServerConnectionStatus

  data object Connected : SubtitleServerConnectionStatus

  data object NotConnected : SubtitleServerConnectionStatus

  data object Searching : SubtitleServerConnectionStatus

  data class Error(val message: String) : SubtitleServerConnectionStatus
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val subtitleRepository: SubtitleRepository,
    private val subtitleServerDiscovery: SubtitleServerDiscovery,
) : ViewModel() {

  private val _connectionStatus =
      MutableStateFlow<SubtitleServerConnectionStatus>(SubtitleServerConnectionStatus.Idle)
  val connectionStatus: StateFlow<SubtitleServerConnectionStatus> = _connectionStatus.asStateFlow()

  val subtitleServerAddress =
      settingsRepository.settings
          .map { it.subtitleServerAddress }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  fun updateSubtitleServerAddress(address: String) {
    viewModelScope.launch {
      settingsRepository.updateSettings { it.copy(subtitleServerAddress = address) }
    }
  }

  fun checkSubtitleServerAddress(address: String) {
    viewModelScope.launch {
      _connectionStatus.value = SubtitleServerConnectionStatus.Testing
      val isConnected = subtitleRepository.healthCheck(address)
      _connectionStatus.value =
          if (isConnected) SubtitleServerConnectionStatus.Connected
          else SubtitleServerConnectionStatus.NotConnected
    }
  }

  fun resetConnectionStatus() {
    _connectionStatus.value = SubtitleServerConnectionStatus.Idle
  }

  fun findSubtitleServer() {
    if (_connectionStatus.value == SubtitleServerConnectionStatus.Searching) return

    _connectionStatus.value = SubtitleServerConnectionStatus.Searching

    val timeoutJob = viewModelScope.launch {
      delay(10_000.milliseconds)
      if (_connectionStatus.value == SubtitleServerConnectionStatus.Searching) {
        viewModelScope.launch(Dispatchers.IO) {
          subtitleServerDiscovery.stop()
        }
        _connectionStatus.value =
            SubtitleServerConnectionStatus.Error("Cannot resolve subtitle server")
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      subtitleServerDiscovery.start { ip, port ->
        viewModelScope.launch {
          val fullAddress = "$ip:$port"
          val isConnected = subtitleRepository.healthCheck(fullAddress)
          if (isConnected && _connectionStatus.value == SubtitleServerConnectionStatus.Searching) {
            timeoutJob.cancel()
            viewModelScope.launch(Dispatchers.IO) {
              subtitleServerDiscovery.stop()
            }
            updateSubtitleServerAddress(fullAddress)
            _connectionStatus.value = SubtitleServerConnectionStatus.Connected
          }
        }
      }
    }
  }

  override fun onCleared() {
    viewModelScope.launch(Dispatchers.IO) {
      subtitleServerDiscovery.stop()
    }
  }
}
