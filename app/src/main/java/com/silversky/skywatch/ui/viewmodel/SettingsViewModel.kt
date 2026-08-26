package com.silversky.skywatch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.skywatch.data.repository.SettingsRepository
import com.silversky.skywatch.model.SortBy
import com.silversky.skywatch.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

  val settings =
      settingsRepository.settings.stateIn(
          viewModelScope,
          SharingStarted.WhileSubscribed(5000),
          com.silversky.skywatch.model.Settings(),
      )

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

  fun updateSubtitleFontSize(size: Int) {
    viewModelScope.launch {
      settingsRepository.updateSettings { it.copy(subtitleFontSize = size) }
    }
  }

  fun updateSubtitleFontFamily(family: String) {
    viewModelScope.launch {
      settingsRepository.updateSettings { it.copy(subtitleFontFamily = family) }
    }
  }
}
