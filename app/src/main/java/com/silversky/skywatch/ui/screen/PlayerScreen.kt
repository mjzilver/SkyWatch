package com.silversky.skywatch.ui.screen

import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.silversky.skywatch.ui.component.AudioTrackDialog
import com.silversky.skywatch.ui.component.PlaybackErrorOverlay
import com.silversky.skywatch.ui.component.PlayerControls
import com.silversky.skywatch.ui.component.SpeedDialog
import com.silversky.skywatch.ui.component.SubtitleDialog
import com.silversky.skywatch.ui.theme.SubtitleBackground
import com.silversky.skywatch.ui.theme.SubtitleOutline
import com.silversky.skywatch.ui.theme.SubtitleText
import com.silversky.skywatch.ui.theme.SubtitleWindow
import com.silversky.skywatch.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
) {
  val player = viewModel.player
  val loading = viewModel.loading
  val error = viewModel.error
  val controlsVisible = viewModel.controlsVisible
  val position = viewModel.position
  val duration = viewModel.duration
  val isPlaying = viewModel.isPlaying
  val file = viewModel.file ?: return

  LaunchedEffect(
      viewModel.controlsVisible,
      viewModel.showAudioMenu,
      viewModel.showSubtitleMenu,
      viewModel.showSpeedMenu,
  ) {
    if (
        viewModel.controlsVisible &&
            !viewModel.showAudioMenu &&
            !viewModel.showSubtitleMenu &&
            !viewModel.showSpeedMenu
    ) {
      delay(5_000L)
      viewModel.controlsVisible = false
    }
  }

  BackHandler {
    when {
      viewModel.error != null -> {
        viewModel.back(onBack)
      }

      viewModel.showAudioMenu -> {
        viewModel.showAudioMenu = false
      }

      viewModel.showSubtitleMenu -> {
        viewModel.showSubtitleMenu = false
      }

      viewModel.showSpeedMenu -> {
        viewModel.showSpeedMenu = false
      }

      viewModel.controlsVisible -> {
        viewModel.controlsVisible = false
      }

      else -> {
        viewModel.back(onBack)
      }
    }
  }

  Box(
      modifier =
          Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) {
              return@onPreviewKeyEvent false
            }

            if (viewModel.error != null) {
              return@onPreviewKeyEvent false
            }

            when (event.key) {
              Key.DirectionCenter,
              Key.Enter -> {
                viewModel.controlsVisible = true
                true
              }

              Key.DirectionLeft -> {
                if (!viewModel.controlsVisible) {
                  player.seekBack()
                  viewModel.controlsVisible = true
                  true
                } else {
                  false
                }
              }

              Key.DirectionRight -> {
                if (!viewModel.controlsVisible) {
                  player.seekForward()
                  viewModel.controlsVisible = true
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              focusable = View.FOCUSABLE
            }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
              text = "Loading...",
              color = Color.White,
          )
        }
      }
    }

    if (error != null) {
      PlaybackErrorOverlay(
          message = error,
          onClose = { viewModel.back(onBack) },
      )
    }

    if (controlsVisible && !loading && error == null) {
      PlayerControls(
          player = player,
          file = file,
          position = position,
          duration = duration,
          isPlaying = isPlaying,
          onPlay = { viewModel.togglePlay() },
          onAudio = {
            viewModel.showAudioMenu = true
            viewModel.controlsVisible = true
          },
          onStop = { viewModel.back(onBack) },
          onSubtitles = {
            viewModel.showSubtitleMenu = true
            viewModel.controlsVisible = true
          },
          onSpeed = {
            viewModel.showSpeedMenu = true
            viewModel.controlsVisible = true
          },
          onHideControls = { viewModel.controlsVisible = false },
      )
    }

    if (viewModel.showAudioMenu) {
      AudioTrackDialog(
          player = player,
          onDismiss = {
            viewModel.savePlaybackState()
            viewModel.showAudioMenu = false
            viewModel.controlsVisible = true
          },
      )
    }

    if (viewModel.showSubtitleMenu) {
      SubtitleDialog(
          player = player,
          filename = file.name,
          onDownloadSubtitle = { subtitle ->
            viewModel.downloadAndLoadSubtitle(subtitle.id, subtitle.name)
          },
          onDismiss = {
            viewModel.savePlaybackState()
            viewModel.showSubtitleMenu = false
            viewModel.controlsVisible = true
          },
      )
    }

    if (viewModel.showSpeedMenu) {
      SpeedDialog(
          player = player,
          onDismiss = {
            viewModel.showSpeedMenu = false
            viewModel.controlsVisible = true
          },
      )
    }
  }
}
