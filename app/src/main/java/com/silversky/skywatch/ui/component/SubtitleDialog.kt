package com.silversky.skywatch.ui.component

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.silversky.skywatch.model.SubtitleResult
import com.silversky.skywatch.ui.viewmodel.SubtitleViewModel

@OptIn(UnstableApi::class, ExperimentalTvMaterial3Api::class)
@Composable
internal fun SubtitleDialog(
    player: ExoPlayer,
    filename: String,
    onDownloadSubtitle: (SubtitleResult) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SubtitleViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Local", "Online")

  LaunchedEffect(player, filename) {
    viewModel.initialize(player, filename)
  }

  Dialog(
      onDismissRequest = onDismiss,
      properties =
          DialogProperties(
              dismissOnBackPress = true,
              dismissOnClickOutside = true,
          ),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth(0.8f)
                .background(
                    Color(0xFF202020),
                    RoundedCornerShape(12.dp),
                )
                .padding(32.dp),
    ) {
      Text(
          text = "Subtitles",
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(16.dp))

      TabRow(
          selectedTabIndex = selectedTabIndex,
          modifier = Modifier.fillMaxWidth(),
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
              selected = selectedTabIndex == index,
              onFocus = { selectedTabIndex = index },
              onClick = { selectedTabIndex = index },
          ) {
            Text(
                text = title,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Column(modifier = Modifier.weight(1f, fill = false)) {
        when (selectedTabIndex) {
          0 -> { // Local
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
              item {
                TrackButton(
                    text = "Off",
                    selected = !player.currentTracks.isTypeSelected(C.TRACK_TYPE_TEXT),
                    onClick = {
                      viewModel.disableSubtitles()
                      onDismiss()
                    },
                )
              }

              items(uiState.localTracks.indices.toList()) { groupIndex ->
                val group = uiState.localTracks[groupIndex]
                for (trackIndex in 0 until group.length) {
                  val format = group.getTrackFormat(trackIndex)
                  val label = format.label ?: format.language ?: "Track ${trackIndex + 1}"

                  TrackButton(
                      text = label,
                      selected = group.isTrackSelected(trackIndex),
                      onClick = {
                        viewModel.selectTrack(groupIndex, trackIndex)
                        onDismiss()
                      },
                  )
                }
              }
            }
          }
          1 -> { // Online
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
              item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                  PlayerButton(
                      text = if (uiState.isSearching) "Searching..." else "Search subtitle server",
                      onClick = { viewModel.searchOnline() },
                      modifier = Modifier.fillMaxWidth(),
                      content = {
                        if (uiState.isSearching) {
                          CircularProgressIndicator(
                              modifier = Modifier.size(16.dp),
                              strokeWidth = 2.dp,
                              color = Color.White,
                          )
                        }
                      },
                  )
                }
              }

              val results = uiState.onlineSubtitles
              val error = uiState.error

              if (!uiState.isSearching && uiState.searchRequested) {
                if (error != null) {
                  item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                  }
                } else if (results != null) {
                  if (results.isEmpty()) {
                    item {
                      Text(
                          text = "No online subtitles found.",
                          color = Color.LightGray,
                          modifier = Modifier.padding(vertical = 16.dp),
                      )
                    }
                  } else {
                    items(results) { subtitle ->
                      TrackButton(
                          text = subtitle.name,
                          selected = false,
                          onClick = { onDownloadSubtitle(subtitle) },
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      PlayerButton(
          text = "Close",
          onClick = onDismiss,
          modifier = Modifier.align(Alignment.End),
      )
    }
  }
}
