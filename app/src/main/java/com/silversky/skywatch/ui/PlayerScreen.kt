package com.silversky.skywatch.ui

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.skywatch.media.createSmbPlayer
import com.silversky.skywatch.media.prepareSmbMediaSource
import com.silversky.skywatch.ui.theme.SubtitleBackground
import com.silversky.skywatch.ui.theme.SubtitleOutline
import com.silversky.skywatch.ui.theme.SubtitleText
import com.silversky.skywatch.ui.theme.SubtitleWindow
import com.silversky.skywatch.utils.PlaybackPositionStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerScreen(
    client: SmbClient,
    shareName: String,
    file: SmbEntry,
    logger: Logger,
    onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val player = remember {
    createSmbPlayer(context)
  }

  var loading by remember {
    mutableStateOf(true)
  }

  var error by remember {
    mutableStateOf<String?>(null)
  }

  var controlsVisible by remember {
    mutableStateOf(true)
  }

  var showAudioMenu by remember {
    mutableStateOf(false)
  }

  var showSubtitleMenu by remember {
    mutableStateOf(false)
  }

  var showSpeedMenu by remember {
    mutableStateOf(false)
  }

  var position by remember {
    mutableLongStateOf(0L)
  }

  var duration by remember {
    mutableLongStateOf(0L)
  }

  var isPlaying by remember {
    mutableStateOf(false)
  }

  val playbackPositionStore = PlaybackPositionStore(context)

  LaunchedEffect(shareName, file.path) {
    loading = true
    error = null

    try {
      logger.info("Starting playback: //$shareName/${file.path}")

      val mediaSource =
          prepareSmbMediaSource(
              smbClient = client,
              shareName = shareName,
              path = file.path,
          )

      val savedPosition =
          playbackPositionStore.getPosition(client.server!!.ipAddress, shareName, file.path)

      player.setMediaSource(mediaSource)
      player.prepare()

      if (savedPosition > 0L) {
        player.seekTo(savedPosition)
      }
      player.playWhenReady = true

      loading = false
    } catch (e: Exception) {
      logger.error(
          "Failed to start playback: ${file.name}",
          e,
      )

      loading = false
      error = e.message ?: "Failed to start playback"
    }
  }

  LaunchedEffect(player, file.path) {
    while (isActive) {
      position = player.currentPosition.coerceAtLeast(0L)
      duration = player.duration.takeIf { it > 0L } ?: 0L
      isPlaying = player.isPlaying

      if (player.isPlaying) {
        playbackPositionStore.savePosition(
            ip = client.server!!.ipAddress,
            share = shareName,
            path = file.path,
            position = player.currentPosition,
        )
      }

      delay(5_000.milliseconds)
    }
  }
  LaunchedEffect(
      controlsVisible,
      showAudioMenu,
      showSubtitleMenu,
      showSpeedMenu,
  ) {
    if (controlsVisible && !showAudioMenu && !showSubtitleMenu && !showSpeedMenu) {
      delay(5_000.milliseconds)
      controlsVisible = false
    }
  }

  BackHandler {
    when {
      showAudioMenu -> {
        showAudioMenu = false
      }

      showSubtitleMenu -> {
        showSubtitleMenu = false
      }

      showSpeedMenu -> {
        showSpeedMenu = false
      }

      controlsVisible -> {
        controlsVisible = false
      }

      else -> {
        player.stop()
        onBack()
      }
    }
  }

  DisposableEffect(player) {
    onDispose {
      logger.debug("Releasing player: ${file.name}")

      player.stop()
      player.clearMediaItems()
      player.release()
    }
  }

  Box(
      modifier =
          Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) {
              return@onPreviewKeyEvent false
            }

            when (event.key) {
              Key.DirectionCenter,
              Key.Enter -> {
                controlsVisible = true
                true
              }

              Key.DirectionLeft -> {
                if (!controlsVisible) {
                  player.seekBack()
                  controlsVisible = true
                  true
                } else {
                  false
                }
              }

              Key.DirectionRight -> {
                if (!controlsVisible) {
                  player.seekForward()
                  controlsVisible = true
                  true
                } else {
                  false
                }
              }

              else -> false
            }
          }
  ) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
          PlayerView(viewContext).apply {
            useController = false

            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)

            keepScreenOn = true

            focusable = View.FOCUSABLE
            isFocusableInTouchMode = true

            subtitleView?.apply {
              setStyle(
                  CaptionStyleCompat(
                      SubtitleText,
                      SubtitleBackground,
                      SubtitleWindow,
                      CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                      SubtitleOutline,
                      null,
                  )
              )
            }
          }
        },
        update = { view ->
          view.player = player
        },
    )

    if (loading) {
      Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
      ) {
        Text(
            text = "Loading...",
            color = Color.White,
        )
      }
    }

    if (error != null) {
      Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Playback failed",
              color = Color.White,
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
              text = error!!,
              color = Color.LightGray,
          )
        }
      }
    }

    if (controlsVisible && !loading && error == null) {
      PlayerControls(
          player = player,
          file = file,
          position = position,
          duration = duration,
          isPlaying,
          onPlay = {
            if (player.isPlaying) {
              player.pause()
            } else {
              try {
                client.ensureConnected()
                player.play()
              } catch (e: Exception) {
                logger.error("Failed to reconnect SMB", e)
              }
            }
          },
          onAudio = {
            showAudioMenu = true
            controlsVisible = true
          },
          onStop = {
            val currentPosition = player.currentPosition

            scope.launch {
              playbackPositionStore.savePosition(
                  ip = client.server!!.ipAddress,
                  share = shareName,
                  path = file.path,
                  position = currentPosition,
              )
            }
            player.stop()
            onBack()
          },
          onSubtitles = {
            showSubtitleMenu = true
            controlsVisible = true
          },
          onSpeed = {
            showSpeedMenu = true
            controlsVisible = true
          },
      )
    }

    if (showAudioMenu) {
      AudioTrackDialog(
          player = player,
          onDismiss = {
            showAudioMenu = false
            controlsVisible = true
          },
      )
    }

    if (showSubtitleMenu) {
      SubtitleTrackDialog(
          player = player,
          onDismiss = {
            showSubtitleMenu = false
            controlsVisible = true
          },
      )
    }

    if (showSpeedMenu) {
      SpeedDialog(
          player = player,
          onDismiss = {
            showSpeedMenu = false
            controlsVisible = true
          },
      )
    }
  }
}

@Composable
private fun PlayerControls(
    player: ExoPlayer,
    file: SmbEntry,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onAudio: () -> Unit,
    onStop: () -> Unit,
    onSubtitles: () -> Unit,
    onSpeed: () -> Unit,
) {
  val playFocus = remember {
    FocusRequester()
  }

  LaunchedEffect(Unit) {
    playFocus.requestFocus()
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(
                    horizontal = 48.dp,
                    vertical = 24.dp,
                )
    ) {
      Text(
          text = file.name,
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = formatTime(position),
            color = Color.White,
        )

        Slider(
            value =
                if (duration > 0L) {
                  position.coerceIn(0L, duration).toFloat()
                } else {
                  0f
                },
            onValueChange = { value ->
              player.seekTo(value.roundToInt().toLong())
            },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 16.dp),
        )

        Text(
            text = formatTime(duration),
            color = Color.White,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        PlayerButton(
            text = "-10",
            onClick = {
              player.seekBack()
            },
        )

        PlayerButton(
            text =
                if (isPlaying) {
                  "Pause"
                } else {
                  "Play"
                },
            modifier = Modifier.focusRequester(playFocus),
            onClick = onPlay,
        )

        PlayerButton(
            text = "Stop",
            onClick = onStop,
        )

        PlayerButton(
            text = "+10",
            onClick = {
              player.seekForward()
            },
        )

        Spacer(modifier = Modifier.width(12.dp))

        PlayerButton(
            text = "Audio",
            onClick = onAudio,
        )

        PlayerButton(
            text = "Subtitles",
            onClick = onSubtitles,
        )

        PlayerButton(
            text = "Speed",
            onClick = onSpeed,
        )
      }
    }
  }
}

@Composable
private fun PlayerButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      modifier = modifier,
  ) {
    Text(text)
  }
}

@Composable
private fun AudioTrackDialog(
    player: ExoPlayer,
    onDismiss: () -> Unit,
) {
  val tracks =
      remember(player.currentTracks) {
        player.currentTracks.groups.filter {
          it.type == C.TRACK_TYPE_AUDIO
        }
      }

  TrackDialog(
      title = "Audio",
      tracks = tracks,
      player = player,
      trackType = C.TRACK_TYPE_AUDIO,
      allowOff = false,
      onDismiss = onDismiss,
  )
}

@Composable
private fun SubtitleTrackDialog(
    player: ExoPlayer,
    onDismiss: () -> Unit,
) {
  val tracks =
      remember(player.currentTracks) {
        player.currentTracks.groups.filter {
          it.type == C.TRACK_TYPE_TEXT
        }
      }

  TrackDialog(
      title = "Subtitles",
      tracks = tracks,
      player = player,
      trackType = C.TRACK_TYPE_TEXT,
      allowOff = true,
      onDismiss = onDismiss,
  )
}

@Composable
private fun TrackDialog(
    title: String,
    tracks: List<Tracks.Group>,
    player: ExoPlayer,
    trackType: Int,
    allowOff: Boolean,
    onDismiss: () -> Unit,
) {
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
            Modifier.fillMaxWidth(0.65f)
                .background(
                    Color(0xFF202020),
                    RoundedCornerShape(12.dp),
                )
                .padding(32.dp)
    ) {
      Text(
          text = title,
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(20.dp))

      if (tracks.isEmpty()) {
        Text(
            text = "No $title tracks available.",
            color = Color.LightGray,
        )
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (allowOff) {
            item {
              TrackButton(
                  text = "Off",
                  selected = !player.currentTracks.isTypeSelected(trackType),
                  onClick = {
                    player.trackSelectionParameters =
                        player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(
                                trackType,
                                true,
                            )
                            .build()

                    onDismiss()
                  },
              )
            }
          }

          items(tracks.indices.toList()) { groupIndex ->
            val group = tracks[groupIndex]

            for (trackIndex in 0 until group.length) {
              val format = group.getTrackFormat(trackIndex)

              val label = format.label ?: format.language ?: "Track ${trackIndex + 1}"

              val selected = group.isTrackSelected(trackIndex)

              TrackButton(
                  text = label,
                  selected = selected,
                  onClick = {
                    player.trackSelectionParameters =
                        player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(
                                trackType,
                                false,
                            )
                            .setOverrideForType(
                                TrackSelectionOverride(
                                    group.mediaTrackGroup,
                                    listOf(trackIndex),
                                )
                            )
                            .build()

                    onDismiss()
                  },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      PlayerButton(
          text = "Close",
          onClick = onDismiss,
      )
    }
  }
}

@Composable
private fun TrackButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
        text =
            if (selected) {
              "✓  $text"
            } else {
              text
            }
    )
  }
}

@Composable
private fun SpeedDialog(
    player: ExoPlayer,
    onDismiss: () -> Unit,
) {
  val speeds =
      listOf(
          0.5f,
          0.75f,
          1.0f,
          1.25f,
          1.5f,
          2.0f,
      )

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
            Modifier.fillMaxWidth(0.55f)
                .background(
                    Color(0xFF202020),
                    RoundedCornerShape(12.dp),
                )
                .padding(32.dp)
    ) {
      Text(
          text = "Playback Speed",
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(20.dp))

      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(speeds) { speed ->
          TrackButton(
              text = "${speed}x",
              selected = player.playbackParameters.speed == speed,
              onClick = {
                player.setPlaybackSpeed(speed)
                onDismiss()
              },
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      PlayerButton(
          text = "Close",
          onClick = onDismiss,
      )
    }
  }
}

private fun formatTime(milliseconds: Long): String {
  if (milliseconds <= 0L) {
    return "00:00"
  }

  val totalSeconds = milliseconds / 1_000

  val hours = totalSeconds / 3_600
  val minutes = (totalSeconds % 3_600) / 60
  val seconds = totalSeconds % 60

  return if (hours > 0) {
    "%d:%02d:%02d"
        .format(
            hours,
            minutes,
            seconds,
        )
  } else {
    "%02d:%02d"
        .format(
            minutes,
            seconds,
        )
  }
}
