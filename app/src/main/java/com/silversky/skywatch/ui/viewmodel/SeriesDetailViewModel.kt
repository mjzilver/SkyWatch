package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.model.EpisodeInfo
import com.silversky.skywatch.data.remote.SmbConnectionManager
import com.silversky.skywatch.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SeriesDetailViewModel
@Inject
constructor(
    private val connectionManager: SmbConnectionManager,
    private val mediaRepository: MediaRepository,
) : ViewModel() {

  var episodes by mutableStateOf<List<EpisodeInfo>>(emptyList())
    private set

  val title: String
    get() = connectionManager.selectedSeriesTitle ?: ""

  init {
    loadEpisodes()
  }

  private fun loadEpisodes() {
    val serverIp = connectionManager.selectedServer?.ipAddress ?: return
    val share = connectionManager.selectedShare?.shareName ?: return
    val currentTitle = title
    if (currentTitle.isEmpty()) return

    viewModelScope.launch {
      val allMedia = mediaRepository.getMediaForShare(serverIp, share)
      episodes =
          allMedia
              .filterIsInstance<EpisodeInfo>()
              .filter { it.title == currentTitle }
              .sortedWith(compareBy({ it.season }, { it.episode }))
    }
  }

  fun selectEpisode(episode: EpisodeInfo, onSelected: () -> Unit) {
    connectionManager.selectFileByPath(
        episode.entryPath,
        "${episode.title} S${episode.season}E${episode.episode}",
    )
    onSelected()
  }
}
