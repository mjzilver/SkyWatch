package com.silversky.skywatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silversky.core.logger.Logger
import com.silversky.core.model.EpisodeInfo
import com.silversky.core.model.MediaInfo
import com.silversky.core.model.MovieInfo
import com.silversky.core.model.SmbEntry
import com.silversky.core.model.SmbEntryType
import com.silversky.skywatch.data.local.PlaybackState
import com.silversky.skywatch.data.local.PlaybackStateStore
import com.silversky.skywatch.data.remote.SmbConnectionManager
import com.silversky.skywatch.data.repository.MediaRepository
import com.silversky.skywatch.data.repository.SettingsRepository
import com.silversky.skywatch.model.SortBy
import com.silversky.skywatch.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BrowserTab {
  Folders,
  Movies,
  Series,
}

data class MediaGroup(
    val title: String,
    val year: Int?,
    val items: List<MediaInfo>,
)

@HiltViewModel
class FileBrowserViewModel
@Inject
constructor(
    private val connectionManager: SmbConnectionManager,
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
    val playbackStateStore: PlaybackStateStore,
    private val logger: Logger,
) : ViewModel() {

  var selectedTab by mutableStateOf(BrowserTab.Folders)
    private set

  var mediaItems by mutableStateOf<List<MediaInfo>>(emptyList())
    private set

  var movieGroups by mutableStateOf<List<MediaGroup>>(emptyList())
    private set

  var seriesGroups by mutableStateOf<List<MediaGroup>>(emptyList())
    private set

  var movieVersionsToPick by mutableStateOf<List<MovieInfo>?>(null)
    private set

  var isScanning by mutableStateOf(false)
    private set

  var currentPath by mutableStateOf("")
    private set

  var entries by mutableStateOf<List<SmbEntry>>(emptyList())
    private set

  var resumeEntries by mutableStateOf<Map<String, PlaybackState>>(emptyMap())
    private set

  var mediaResumeStates by mutableStateOf<Map<String, PlaybackState>>(emptyMap())
    private set

  var loading by mutableStateOf(false)
    private set

  var error by mutableStateOf<String?>(null)
    private set

  val client
    get() = connectionManager.smbClient

  val server
    get() = connectionManager.selectedServer

  val shareName
    get() = connectionManager.selectedShare?.shareName

  private var rawEntries: List<SmbEntry> = emptyList()

  fun loadEntries() {
    val client = client ?: return
    val shareName = shareName ?: return

    loading = true
    error = null

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val result =
            client
                .list(
                    shareName = shareName,
                    path = currentPath,
                )
                .filter { !it.isHidden }

        rawEntries = result

        val resumeMap = playbackStateStore.getForShare(server?.ipAddress ?: "", shareName)

        withContext(Dispatchers.Main) {
          resumeEntries = resumeMap
          applySorting()
          loading = false
        }
      } catch (e: Exception) {
        logger.error("Failed to list //$shareName/$currentPath", e)
        withContext(Dispatchers.Main) {
          rawEntries = emptyList()
          entries = emptyList()
          error = e.message ?: "Failed to load directory"
          loading = false
        }
      }
    }
  }

  init {
    viewModelScope.launch {
      settingsRepository.settings.collect {
        if (!loading && rawEntries.isNotEmpty()) {
          applySorting()
        }
      }
    }
  }

  private fun applySorting() {
    val settings = settingsRepository.settings.value

    val sortedResult = rawEntries.sortedWith { a, b ->
      if (settings.foldersFirst && a.type != b.type) {
        if (a.type == SmbEntryType.Directory) -1 else 1
      } else {
        val comparison =
            when (settings.sortBy) {
              SortBy.Name -> a.name.compareTo(b.name, ignoreCase = true)

              SortBy.DateModified -> a.dateModified.compareTo(b.dateModified)

              SortBy.Size -> a.size.compareTo(b.size)
            }

        if (settings.sortOrder == SortOrder.Ascending) {
          comparison
        } else {
          -comparison
        }
      }
    }

    entries = sortedResult
  }

  fun selectFile(file: SmbEntry, onFileSelected: () -> Unit) {
    connectionManager.onFileSelected(file)
    onFileSelected()
  }

  fun startSeriesSelection(title: String, onSeriesSelected: () -> Unit) {
    connectionManager.onSeriesSelected(title)
    onSeriesSelected()
  }

  fun pickMovieVersion(versions: List<MovieInfo>) {
    movieVersionsToPick = versions
  }

  fun dismissMovieVersionPicker() {
    movieVersionsToPick = null
  }

  fun navigateTo(path: String) {
    currentPath = path
    loadEntries()
  }

  fun goBack(onBack: () -> Unit) {
    if (currentPath.isEmpty()) {
      connectionManager.clearShare()
      onBack()
    } else {
      currentPath = parentPath(currentPath)
      loadEntries()
    }
  }

  private fun parentPath(path: String): String {
    val normalized = path.trimEnd('\\')
    val index = normalized.lastIndexOf('\\')
    return if (index < 0) "" else normalized.substring(0, index)
  }

  fun selectTab(tab: BrowserTab) {
    selectedTab = tab
    if (tab == BrowserTab.Movies || tab == BrowserTab.Series) {
      loadMedia()
    }
  }

  fun loadMedia() {
    val serverIp = server?.ipAddress ?: return
    val share = shareName ?: return

    viewModelScope.launch {
      val cached = mediaRepository.getMediaForShare(serverIp, share)
      mediaItems = cached
      updateGroups()
      loadMediaPlaybackStates()
      startScan()
    }
  }

  private fun updateGroups() {
    val sorted = applyMediaSorting(mediaItems)

    val movies = sorted.filterIsInstance<MovieInfo>()
    movieGroups = groupSmartly(movies)

    val series = sorted.filterIsInstance<EpisodeInfo>()
    seriesGroups = groupSmartly(series)
  }

  private fun <T : MediaInfo> groupSmartly(items: List<T>): List<MediaGroup> {
    val grouped = items.groupBy { it.title.lowercase().trim() }

    return grouped
        .map { (_, groupItems) ->
          val bestTitle =
              groupItems.groupBy { it.title }.maxByOrNull { it.value.size }?.key
                  ?: groupItems.first().title

          val bestYear = groupItems.mapNotNull { it.year }.minOrNull()

          MediaGroup(
              title = bestTitle,
              year = bestYear,
              items = groupItems,
          )
        }
        .sortedBy { it.title.lowercase() }
  }

  private suspend fun loadMediaPlaybackStates() {
    val serverIp = server?.ipAddress ?: return
    val share = shareName ?: return

    val states =
        withContext(Dispatchers.IO) {
          playbackStateStore.getForShare(serverIp, share)
        }

    withContext(Dispatchers.Main) {
      mediaResumeStates = states
    }
  }

  fun startScan() {
    val client = client ?: return
    val serverIp = server?.ipAddress ?: return
    val share = shareName ?: return

    if (isScanning) return

    viewModelScope.launch {
      isScanning = true
      try {
        val result = mediaRepository.scanAndSave(client, serverIp, share)
        mediaItems = result
        updateGroups()
        loadMediaPlaybackStates()
      } catch (e: Exception) {
        logger.error("Failed to scan media", e)
      } finally {
        isScanning = false
      }
    }
  }

  private fun applyMediaSorting(items: List<MediaInfo>): List<MediaInfo> {
    val settings = settingsRepository.settings.value

    return items.sortedWith { a, b ->
      val comparison =
          when (settings.sortBy) {
            SortBy.Name -> a.title.compareTo(b.title, ignoreCase = true)
            else -> 0
          }

      if (settings.sortOrder == SortOrder.Ascending) {
        comparison
      } else {
        -comparison
      }
    }
  }
}
