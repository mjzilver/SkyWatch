package com.silversky.skywatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.silversky.core.smb.SmbEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun PlaybackErrorOverlay(
    message: String,
    onClose: () -> Unit,
) {
  val closeFocus = remember {
    FocusRequester()
  }

  LaunchedEffect(Unit) {
    closeFocus.requestFocus()
  }

  Box(
      modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.90f)),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth(0.65f)
                .background(
                    Color(0xFF202020),
                    RoundedCornerShape(16.dp),
                )
                .padding(
                    horizontal = 40.dp,
                    vertical = 32.dp,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = "Playback failed",
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
          text = message,
          color = Color.LightGray,
      )

      Spacer(modifier = Modifier.height(28.dp))

      Button(
          onClick = onClose,
          modifier = Modifier.focusRequester(closeFocus),
      ) {
        Text("Close")
      }
    }
  }
}

@Composable
internal fun PlayerControls(
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
    onHideControls: () -> Unit,
) {
  val playFocus = remember {
    FocusRequester()
  }

  val sliderFocus = remember {
    FocusRequester()
  }

  LaunchedEffect(Unit) {
    playFocus.requestFocus()
  }

  var seeking by remember {
    mutableStateOf(false)
  }

  var seekDirection by remember {
    mutableIntStateOf(0)
  }

  var seekSpeed by remember {
    mutableLongStateOf(10_000L)
  }

  var seekJob by remember {
    mutableStateOf<Job?>(null)
  }

  var sliderSeeking by remember {
    mutableStateOf(false)
  }

  var sliderPosition by remember {
    mutableLongStateOf(position)
  }

  val scope = rememberCoroutineScope()

  LaunchedEffect(position, sliderSeeking) {
    if (!sliderSeeking) {
      sliderPosition = position
    }
  }

  fun startSeeking(direction: Int) {
    if (seeking) {
      return
    }

    seeking = true
    seekDirection = direction
    seekSpeed = 10_000L

    seekJob = scope.launch {
      var heldTime = 0L

      while (isActive) {
        val amount = seekSpeed

        val newPosition =
            if (seekDirection < 0) {
              (player.currentPosition - amount).coerceAtLeast(0L)
            } else {
              (player.currentPosition + amount).coerceAtMost(duration)
            }

        player.seekTo(newPosition)

        delay(100L.milliseconds)

        heldTime += 100L

        seekSpeed =
            when {
              heldTime > 3_000L -> 60_000L
              heldTime > 2_000L -> 40_000L
              heldTime > 1_000L -> 20_000L
              else -> 10_000L
            }
      }
    }
  }

  fun stopSeeking() {
    seeking = false
    seekJob?.cancel()
    seekJob = null
  }

  Box(
      modifier = Modifier.fillMaxSize(),
  ) {
    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(
                    horizontal = 48.dp,
                    vertical = 24.dp,
                ),
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
            text =
                formatTime(
                    if (sliderSeeking) {
                      sliderPosition
                    } else {
                      position
                    }
                ),
            color = Color.White,
        )

        Box(
            modifier =
                Modifier.weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 16.dp)
                    .focusRequester(sliderFocus)
                    .focusable()
                    .onKeyEvent { event ->
                      when (event.key) {
                        Key.DirectionLeft if event.type == KeyEventType.KeyDown -> {
                          startSeeking(-1)
                          true
                        }

                        Key.DirectionRight if event.type == KeyEventType.KeyDown -> {
                          startSeeking(1)
                          true
                        }

                        Key.DirectionLeft if event.type == KeyEventType.KeyUp -> {
                          stopSeeking()
                          true
                        }

                        Key.DirectionRight if event.type == KeyEventType.KeyUp -> {
                          stopSeeking()
                          true
                        }

                        Key.DirectionDown if event.type == KeyEventType.KeyDown -> {
                          stopSeeking()
                          playFocus.requestFocus()
                          true
                        }

                        Key.DirectionUp if event.type == KeyEventType.KeyDown -> {
                          stopSeeking()
                          onHideControls()
                          true
                        }

                        else -> false
                      }
                    },
        ) {
          Slider(
              value =
                  if (duration > 0L) {
                    (if (sliderSeeking) {
                          sliderPosition
                        } else {
                          position
                        })
                        .coerceIn(0L, duration)
                        .toFloat()
                  } else {
                    0f
                  },
              onValueChange = { value ->
                sliderSeeking = true
                sliderPosition = value.roundToInt().toLong()
              },
              onValueChangeFinished = {
                player.seekTo(sliderPosition)
                sliderSeeking = false
              },
              valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
              modifier =
                  Modifier.fillMaxWidth().focusProperties {
                    canFocus = false
                  },
          )
        }

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
internal fun PlayerButton(
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
