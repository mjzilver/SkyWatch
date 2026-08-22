package com.silversky.skywatch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.silversky.skywatch.data.repository.SubtitleRepository
import com.silversky.skywatch.model.SubtitleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubtitleUiState(
    val localTracks: List<Tracks.Group> = emptyList(),
    val onlineSubtitles: List<SubtitleResult>? = null,
    val isSearching: Boolean = false,
    val searchRequested: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SubtitleViewModel @Inject constructor(private val subtitleRepository: SubtitleRepository) :
    ViewModel() {

  private val _uiState = MutableStateFlow(SubtitleUiState())
  val uiState: StateFlow<SubtitleUiState> = _uiState.asStateFlow()

  private var player: ExoPlayer? = null
  private var filename: String = ""

  private val playerListener =
      object : Player.Listener {
        override fun onTracksChanged(tracks: Tracks) {
          updateLocalTracks()
        }
      }

  fun initialize(player: ExoPlayer, filename: String) {
    if (this.player != player) {
      this.player?.removeListener(playerListener)
      this.player = player
      player.addListener(playerListener)
    }
    this.filename = filename
    updateLocalTracks()
  }

  override fun onCleared() {
    player?.removeListener(playerListener)
    player = null
  }

  fun updateLocalTracks() {
    val p = player ?: return
    val tracks = p.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    _uiState.value = _uiState.value.copy(localTracks = tracks)
  }

  fun searchOnline() {
    if (_uiState.value.isSearching) return

    _uiState.value =
        _uiState.value.copy(
            isSearching = true,
            searchRequested = true,
            error = null,
        )

    viewModelScope.launch {
      try {
        val result = subtitleRepository.search(filename)
        if (result == null) {
          _uiState.value =
              _uiState.value.copy(
                  onlineSubtitles = emptyList(),
                  isSearching = false,
                  error = "Subtitles not found",
              )
        } else {
          _uiState.value =
              _uiState.value.copy(
                  onlineSubtitles = result.subtitles,
                  isSearching = false,
              )
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                onlineSubtitles = emptyList(),
                isSearching = false,
                error = e.message ?: "Failed to search subtitles",
            )
      }
    }
  }

  fun selectTrack(groupIndex: Int, trackIndex: Int) {
    val p = player ?: return
    val groups = p.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    if (groupIndex in groups.indices) {
      val group = groups[groupIndex]
      p.trackSelectionParameters =
          p.trackSelectionParameters
              .buildUpon()
              .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
              .setOverrideForType(
                  TrackSelectionOverride(
                      group.mediaTrackGroup,
                      listOf(trackIndex),
                  )
              )
              .build()
    }
  }

  fun disableSubtitles() {
    player?.let { p ->
      p.trackSelectionParameters =
          p.trackSelectionParameters
              .buildUpon()
              .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
              .build()
    }
  }
}
