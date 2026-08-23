package com.silversky.skywatch.ui.component

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
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
import com.silversky.skywatch.data.local.CachedSubtitle
import com.silversky.skywatch.model.SubtitleResult
import com.silversky.skywatch.ui.viewmodel.SubtitleViewModel
import kotlin.math.roundToInt

@OptIn(UnstableApi::class, ExperimentalTvMaterial3Api::class)
@Composable
internal fun SubtitleDialog(
    player: ExoPlayer,
    filename: String,
    subtitleOffset: Long,
    externalSubtitleName: String?,
    onOffsetChange: (Long) -> Unit,
    onClearExternalSubtitles: () -> Unit,
    onDownloadSubtitle: (SubtitleResult) -> Unit,
    onSelectCachedSubtitle: (CachedSubtitle) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SubtitleViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var selectedTab by remember { mutableStateOf(SubtitleTab.Local) }
  val focusManager = LocalFocusManager.current
  val resetFocusRequester = remember { FocusRequester() }
  val tabFocusRequesters = remember { SubtitleTab.entries.map { FocusRequester() } }

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
            Modifier.fillMaxWidth(0.6f)
                .background(
                    Color(0xFF202020),
                    RoundedCornerShape(12.dp),
                )
                .padding(24.dp),
    ) {
      Text(
          text = "Subtitles",
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(16.dp))

      TabRow(
          selectedTabIndex = selectedTab.ordinal,
          modifier = Modifier.fillMaxWidth(),
      ) {
        SubtitleTab.entries.forEach { tab ->
          Tab(
              selected = selectedTab == tab,
              onFocus = { selectedTab = tab },
              onClick = { selectedTab = tab },
              modifier = Modifier.focusRequester(tabFocusRequesters[tab.ordinal]),
          ) {
            Text(
                text = tab.label,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Column(modifier = Modifier.weight(1f, fill = false)) {
        when (selectedTab) {
          SubtitleTab.Local -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
              item {
                TrackButton(
                    text = "Off",
                    selected =
                        !player.currentTracks.isTypeSelected(C.TRACK_TYPE_TEXT) &&
                            externalSubtitleName == null,
                    onClick = {
                      onClearExternalSubtitles()
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
                      selected = group.isTrackSelected(trackIndex) && externalSubtitleName == null,
                      onClick = {
                        onClearExternalSubtitles()
                        viewModel.selectTrack(groupIndex, trackIndex)
                        onDismiss()
                      },
                  )
                }
              }

              if (uiState.cachedSubtitles.isNotEmpty()) {
                items(uiState.cachedSubtitles) { cached ->
                  TrackButton(
                      text = "${cached.name} [cached]",
                      selected = externalSubtitleName == cached.name,
                      onClick = {
                        onSelectCachedSubtitle(cached)
                        onDismiss()
                      },
                  )
                }
              }
            }
          }
          SubtitleTab.Online -> {
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
          SubtitleTab.Offset -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              var isSliderFocused by remember { mutableStateOf(false) }

              Text(
                  text = "Current Offset: ${(subtitleOffset / 100.0).roundToInt() / 10.0}s",
                  style = MaterialTheme.typography.bodyLarge,
                  color = if (isSliderFocused) MaterialTheme.colorScheme.primary else Color.White,
              )

              Box(
                  modifier =
                      Modifier.fillMaxWidth(0.8f)
                          .padding(horizontal = 16.dp)
                          .onFocusChanged { isSliderFocused = it.isFocused }
                          .focusable()
                          .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                              when (event.key) {
                                Key.DirectionDown -> {
                                  resetFocusRequester.requestFocus()
                                  return@onPreviewKeyEvent true
                                }
                                Key.DirectionUp -> {
                                  tabFocusRequesters[selectedTab.ordinal].requestFocus()
                                  return@onPreviewKeyEvent true
                                }
                                Key.DirectionLeft,
                                Key.DirectionRight -> {
                                  val direction = if (event.key == Key.DirectionLeft) -1 else 1
                                  val repeat = event.nativeKeyEvent.repeatCount
                                  val acceleration =
                                      when {
                                        repeat > 60 -> 200
                                        repeat > 40 -> 100
                                        repeat > 20 -> 20
                                        repeat > 5 -> 5
                                        else -> 1
                                      }
                                  val delta = direction * 100L * acceleration
                                  onOffsetChange(
                                      (subtitleOffset + delta).coerceIn(-120000L, 120000L)
                                  )
                                  return@onPreviewKeyEvent true
                                }
                              }
                            }
                            false
                          },
              ) {
                Slider(
                    value = (subtitleOffset / 1000f).coerceIn(-60f, 60f),
                    onValueChange = { onOffsetChange((it * 1000).toLong()) },
                    valueRange = -60f..60f,
                    modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
                    colors =
                        SliderDefaults.colors(
                            thumbColor =
                                if (isSliderFocused) MaterialTheme.colorScheme.primary
                                else Color.Gray,
                            activeTrackColor =
                                if (isSliderFocused) MaterialTheme.colorScheme.primary
                                else Color.Gray,
                        ),
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        if (selectedTab == SubtitleTab.Offset) {
          PlayerButton(
              text = "Reset",
              onClick = { onOffsetChange(0) },
              modifier = Modifier.focusRequester(resetFocusRequester),
          )
          Spacer(modifier = Modifier.width(12.dp))
        }

        PlayerButton(
            text = "Close",
            onClick = onDismiss,
        )
      }
    }
  }
}

private enum class SubtitleTab(val label: String) {
  Local("Local"),
  Online("Online"),
  Offset("Offset"),
}
