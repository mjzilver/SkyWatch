package com.silversky.skywatch.ui

import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.skywatch.media.createSmbPlayer
import com.silversky.skywatch.media.prepareSmbMediaSource
import com.silversky.skywatch.persistence.PlaybackState
import com.silversky.skywatch.persistence.PlaybackStateStore
import com.silversky.skywatch.ui.theme.SubtitleBackground
import com.silversky.skywatch.ui.theme.SubtitleOutline
import com.silversky.skywatch.ui.theme.SubtitleText
import com.silversky.skywatch.ui.theme.SubtitleWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    client: SmbClient,
    shareName: String,
    file: SmbEntry,
    logger: Logger,
    playbackStateStore: PlaybackStateStore,
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

  var savedState by remember {
    mutableStateOf<PlaybackState?>(null)
  }

  fun savePlaybackState() {
    scope.launch {
      playbackStateStore.save(
          ip = client.server!!.ipAddress,
          share = shareName,
          path = file.path,
          state =
              PlaybackState(
                  position = player.currentPosition.coerceAtLeast(0L),
                  duration = player.duration.takeIf { it > 0L } ?: 0L,
                  audioTrack =
                      getSelectedTrackId(
                          player,
                          C.TRACK_TYPE_AUDIO,
                      ),
                  subtitleTrack = getSelectedSubtitleTrackId(player),
              ),
      )
    }
  }

  DisposableEffect(player) {
    val listener =
        object : Player.Listener {
          override fun onPlaybackStateChanged(state: Int) {
            logger.debug(
                "PLAYER STATE: ${
                            when (state) {
                                Player.STATE_IDLE -> "IDLE"
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_READY -> "READY"
                                Player.STATE_ENDED -> "ENDED"
                                else -> "UNKNOWN"
                            }
                        }"
            )
          }

          override fun onIsLoadingChanged(isLoading: Boolean) {
            logger.debug("PLAYER LOADING: $isLoading")
          }

          override fun onPlayerError(playbackException: PlaybackException) {
            logger.error(
                "PLAYER ERROR: ${playbackException.errorCodeName}",
                playbackException,
            )

            error =
                when (playbackException.errorCode) {
                  PlaybackException.ERROR_CODE_DECODING_FAILED ->
                      "This video uses a video format or codec that your device cannot decode."

                  PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                      "This video format is not supported by your device."

                  PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                      "The video file appears to be damaged or malformed."

                  PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                      "The network connection to the SMB server was lost."

                  PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                      "The connection to the SMB server timed out."

                  else -> playbackException.message ?: "An unexpected playback error occurred."
                }

            loading = false
            controlsVisible = false
          }
        }

    player.addListener(listener)

    onDispose {
      player.removeListener(listener)
    }
  }

  LaunchedEffect(shareName, file.path) {
    loading = true
    error = null

    try {
      logger.info("Starting playback: //$shareName/${file.path}")

      savedState =
          playbackStateStore.get(
              client.server!!.ipAddress,
              shareName,
              file.path,
          )

      val mediaSource =
          prepareSmbMediaSource(
              smbClient = client,
              shareName = shareName,
              path = file.path,
              logger,
          )

      player.setMediaSource(mediaSource)
      player.prepare()

      val savedPosition = savedState?.position

      if (savedPosition != null && savedPosition > 0L) {
        player.seekTo(savedPosition)
        position = savedPosition
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

  LaunchedEffect(player, savedState) {
    val state = savedState ?: return@LaunchedEffect

    while (player.currentTracks.groups.isEmpty() && isActive) {
      delay(100L.milliseconds)
    }

    if (!isActive) {
      return@LaunchedEffect
    }

    state.audioTrack?.let { id ->
      findTrack(
              player = player,
              trackType = C.TRACK_TYPE_AUDIO,
              id = id,
          )
          ?.let { selection ->
            player.trackSelectionParameters =
                player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(
                        C.TRACK_TYPE_AUDIO,
                        false,
                    )
                    .setOverrideForType(
                        TrackSelectionOverride(
                            selection.group,
                            listOf(selection.index),
                        )
                    )
                    .build()
          }
    }

    state.subtitleTrack?.let { id ->
      if (id == "off") {
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(
                    C.TRACK_TYPE_TEXT,
                    true,
                )
                .build()
      } else {
        findTrack(
                player = player,
                trackType = C.TRACK_TYPE_TEXT,
                id = id,
            )
            ?.let { selection ->
              player.trackSelectionParameters =
                  player.trackSelectionParameters
                      .buildUpon()
                      .setTrackTypeDisabled(
                          C.TRACK_TYPE_TEXT,
                          false,
                      )
                      .setOverrideForType(
                          TrackSelectionOverride(
                              selection.group,
                              listOf(selection.index),
                          )
                      )
                      .build()
            }
      }
    }
  }

  LaunchedEffect(player, file.path) {
    while (isActive) {
      position = player.currentPosition.coerceAtLeast(0L)
      duration = player.duration.takeIf { it > 0L } ?: 0L
      isPlaying = player.isPlaying

      delay(250L.milliseconds)
    }
  }

  LaunchedEffect(player, file.path) {
    while (isActive) {
      if (player.isPlaying) {
        savePlaybackState()
      }

      delay(5_000L.milliseconds)
    }
  }

  LaunchedEffect(
      controlsVisible,
      showAudioMenu,
      showSubtitleMenu,
      showSpeedMenu,
  ) {
    if (controlsVisible && !showAudioMenu && !showSubtitleMenu && !showSpeedMenu) {
      delay(5_000L.milliseconds)
      controlsVisible = false
    }
  }

  BackHandler {
    when {
      error != null -> {
        savePlaybackState()
        player.stop()
        onBack()
      }

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
        savePlaybackState()
        player.stop()
        onBack()
      }
    }
  }

  DisposableEffect(player) {
    onDispose {
      logger.debug("Releasing player: ${file.name}")

      savePlaybackState()

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

            if (error != null) {
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

            setShowBuffering(
                PlayerView.SHOW_BUFFERING_WHEN_PLAYING,
            )

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
          message = error!!,
          onClose = {
            savePlaybackState()
            player.stop()
            onBack()
          },
      )
    }

    if (controlsVisible && !loading && error == null) {
      PlayerControls(
          player = player,
          file = file,
          position = position,
          duration = duration,
          isPlaying = isPlaying,
          onPlay = {
            if (player.isPlaying) {
              player.pause()
            } else {
              try {
                client.ensureConnected()
                player.play()
              } catch (e: Exception) {
                logger.error(
                    "Failed to reconnect SMB",
                    e,
                )
              }
            }
          },
          onAudio = {
            showAudioMenu = true
            controlsVisible = true
          },
          onStop = {
            savePlaybackState()
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
          onHideControls = {
            controlsVisible = false
          },
      )
    }

    if (showAudioMenu) {
      AudioTrackDialog(
          player = player,
          onDismiss = {
            savePlaybackState()
            showAudioMenu = false
            controlsVisible = true
          },
      )
    }

    if (showSubtitleMenu) {
      SubtitleTrackDialog(
          player = player,
          onDismiss = {
            savePlaybackState()
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
