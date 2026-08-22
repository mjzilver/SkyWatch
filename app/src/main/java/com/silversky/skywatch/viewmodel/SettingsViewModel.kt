package com.silversky.skywatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.skywatch.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settingsManager: SettingsManager) :
    ViewModel() {

  val subtitleServerAddress =
      settingsManager.settings
          .map { it.subtitleServerAddress }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  fun updateSubtitleServerAddress(address: String) {
    viewModelScope.launch {
      settingsManager.updateSettings { it.copy(subtitleServerAddress = address) }
    }
  }
}
