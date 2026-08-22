package com.silversky.skywatch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.skywatch.data.repository.SettingsRepository
import com.silversky.skywatch.data.repository.SubtitleRepository
import com.silversky.skywatch.model.SortBy
import com.silversky.skywatch.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
) : ViewModel() {

  private val _connectionStatus =
      MutableStateFlow<SubtitleServerConnectionStatus>(SubtitleServerConnectionStatus.Idle)
  val connectionStatus: StateFlow<SubtitleServerConnectionStatus> = _connectionStatus.asStateFlow()

  val subtitleServerAddress =
      settingsRepository.settings
          .map { it.subtitleServerAddress }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val settings =
      settingsRepository.settings.stateIn(
          viewModelScope,
          SharingStarted.WhileSubscribed(5000),
          com.silversky.skywatch.model.Settings(),
      )

  init {
    viewModelScope.launch {
      subtitleRepository.isDiscovering.collect { discovering ->
        if (discovering && _connectionStatus.value == SubtitleServerConnectionStatus.Idle) {
          _connectionStatus.value = SubtitleServerConnectionStatus.Searching
        } else if (
            !discovering && _connectionStatus.value == SubtitleServerConnectionStatus.Searching
        ) {
          _connectionStatus.value = SubtitleServerConnectionStatus.Idle
        }
      }
    }

    viewModelScope.launch {
      subtitleRepository.autoDiscoveredAddress.collect { address ->
        if (
            address != null && _connectionStatus.value == SubtitleServerConnectionStatus.Searching
        ) {
          _connectionStatus.value = SubtitleServerConnectionStatus.Connected
          updateSubtitleServerAddress(address)
        }
      }
    }
  }

  fun updateSubtitleServerAddress(address: String) {
    viewModelScope.launch {
      settingsRepository.updateSettings { it.copy(subtitleServerAddress = address) }
    }
  }

  fun updateSortBy(sortBy: SortBy) {
    viewModelScope.launch {
      settingsRepository.updateSettings { it.copy(sortBy = sortBy) }
    }
  }

  fun updateSortOrder(sortOrder: SortOrder) {
    viewModelScope.launch {
      settingsRepository.updateSettings { it.copy(sortOrder = sortOrder) }
    }
  }

  fun updateFoldersFirst(foldersFirst: Boolean) {
    viewModelScope.launch {
      settingsRepository.updateSettings { it.copy(foldersFirst = foldersFirst) }
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
    subtitleRepository.startDiscovery()

    viewModelScope.launch {
      delay(10_000.milliseconds)
      if (_connectionStatus.value == SubtitleServerConnectionStatus.Searching) {
        _connectionStatus.value =
            SubtitleServerConnectionStatus.Error("Cannot resolve subtitle server")
      }
    }
  }
}
