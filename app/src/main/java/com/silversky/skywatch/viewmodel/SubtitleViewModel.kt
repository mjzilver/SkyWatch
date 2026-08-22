package com.silversky.skywatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.silversky.skywatch.subtitle.SubtitleResult
import com.silversky.skywatch.subtitle.SubtitleServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubtitleUiState(
    val localTracks: List<Tracks.Group> = emptyList(),
    val onlineSubtitles: List<SubtitleResult>? = null,
    val isSearching: Boolean = false,
    val searchRequested: Boolean = false,
)

@HiltViewModel
class SubtitleViewModel
@Inject
constructor(private val subtitleServerManager: SubtitleServerManager) : ViewModel() {

  private val _uiState = MutableStateFlow(SubtitleUiState())
  val uiState: StateFlow<SubtitleUiState> = _uiState.asStateFlow()

  private var player: ExoPlayer? = null
  private var filename: String = ""

  fun initialize(player: ExoPlayer, filename: String) {
    this.player = player
    this.filename = filename
    updateLocalTracks()
  }

  fun updateLocalTracks() {
    val p = player ?: return
    val tracks = p.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    _uiState.value = _uiState.value.copy(localTracks = tracks)
  }

  fun searchOnline() {
    if (_uiState.value.isSearching) return

    _uiState.value = _uiState.value.copy(isSearching = true, searchRequested = true)

    viewModelScope.launch {
      val results = subtitleServerManager.search(filename)
      _uiState.value =
          _uiState.value.copy(
              onlineSubtitles = results?.subtitles,
              isSearching = false,
          )
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
