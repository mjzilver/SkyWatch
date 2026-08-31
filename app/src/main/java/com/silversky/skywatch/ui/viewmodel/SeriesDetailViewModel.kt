package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.model.EpisodeInfo
import com.silversky.skywatch.data.local.PlaybackState
import com.silversky.skywatch.data.local.PlaybackStateStore
import com.silversky.skywatch.data.remote.SmbConnectionManager
import com.silversky.skywatch.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SeriesDetailViewModel
@Inject
constructor(
    private val connectionManager: SmbConnectionManager,
    private val mediaRepository: MediaRepository,
    private val playbackStateStore: PlaybackStateStore,
) : ViewModel() {

  var episodes by mutableStateOf<List<EpisodeInfo>>(emptyList())
    private set

  var episodeStates by mutableStateOf<Map<String, PlaybackState>>(emptyMap())
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
      val filteredEpisodes =
          allMedia
              .filterIsInstance<EpisodeInfo>()
              .filter { it.title.equals(currentTitle, ignoreCase = true) }
              .sortedWith(compareBy({ it.season }, { it.episode }))

      episodes = filteredEpisodes
      loadEpisodeStates(serverIp, share)
    }
  }

  private suspend fun loadEpisodeStates(ip: String, share: String) {
    val states =
        withContext(Dispatchers.IO) {
          playbackStateStore.getForShare(ip, share)
        }
    withContext(Dispatchers.Main) {
      episodeStates = states
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
