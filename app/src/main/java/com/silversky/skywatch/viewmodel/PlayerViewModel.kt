package com.silversky.skywatch.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import com.silversky.core.logger.Logger
import com.silversky.skywatch.manager.ConnectionManager
import com.silversky.skywatch.media.createSmbPlayer
import com.silversky.skywatch.media.getSubtitleCacheDir
import com.silversky.skywatch.media.prepareSmbMediaSource
import com.silversky.skywatch.persistence.PlaybackState
import com.silversky.skywatch.persistence.PlaybackStateStore
import com.silversky.skywatch.subtitle.SubtitleServerManager
import com.silversky.skywatch.ui.findTrack
import com.silversky.skywatch.ui.getSelectedSubtitleTrackId
import com.silversky.skywatch.ui.getSelectedTrackId
import com.silversky.skywatch.utils.buildSmbUri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@HiltViewModel
class PlayerViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val connectionManager: ConnectionManager,
    val playbackStateStore: PlaybackStateStore,
    val subtitleServerManager: SubtitleServerManager,
    private val logger: Logger,
) : ViewModel() {

  val player: ExoPlayer = createSmbPlayer(context)

  var loading by mutableStateOf(true)
    private set

  var error by mutableStateOf<String?>(null)
    private set

  var controlsVisible by mutableStateOf(true)

  var showAudioMenu by mutableStateOf(false)

  var showSubtitleMenu by mutableStateOf(false)

  var showSpeedMenu by mutableStateOf(false)

  var position by mutableLongStateOf(0L)
    private set

  var duration by mutableLongStateOf(0L)
    private set

  var isPlaying by mutableStateOf(false)
    private set

  private var savedState: PlaybackState? = null

  val client
    get() = connectionManager.smbClient

  val shareName
    get() = connectionManager.selectedShare

  val file
    get() = connectionManager.selectedFile

  init {
    setupPlayer()
    startPlayback()
    startStateUpdates()
    startAutoSave()
  }

  private fun setupPlayer() {
    player.addListener(
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

          override fun onPlayerError(playbackException: PlaybackException) {
            logger.error("PLAYER ERROR: ${playbackException.errorCodeName}", playbackException)

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
    )
  }

  private fun startPlayback() {
    val share = shareName ?: return
    val smbFile = file ?: return
    val client = client ?: return

    viewModelScope.launch {
      loading = true
      error = null

      try {
        logger.info("Starting playback: //${share}/${smbFile.path}")

        savedState =
            playbackStateStore.get(
                client.server!!.ipAddress,
                share,
                smbFile.path,
            )

        val mediaSource =
            prepareSmbMediaSource(
                context = context,
                smbClient = client,
                shareName = share,
                path = smbFile.path,
                logger = logger,
            )

        player.setMediaSource(mediaSource)
        player.prepare()

        val savedPosition = savedState?.position
        if (savedPosition != null && savedPosition > 0L) {
          player.seekTo(savedPosition)
          position = savedPosition
        }

        applySavedTracks()

        player.playWhenReady = true
        loading = false
      } catch (e: Exception) {
        logger.error("Failed to start playback: ${smbFile.name}", e)
        loading = false
        error = e.message ?: "Failed to start playback"
      }
    }
  }

  private fun applySavedTracks() {
    val state = savedState ?: return
    viewModelScope.launch {
      while (player.currentTracks.groups.isEmpty() && isActive) {
        delay(100L.milliseconds)
      }
      if (!isActive) return@launch

      state.audioTrack?.let { id ->
        findTrack(player, C.TRACK_TYPE_AUDIO, id)?.let { selection ->
          player.trackSelectionParameters =
              player.trackSelectionParameters
                  .buildUpon()
                  .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                  .setOverrideForType(
                      TrackSelectionOverride(selection.group, listOf(selection.index))
                  )
                  .build()
        }
      }

      state.subtitleTrack?.let { id ->
        if (id == "off") {
          player.trackSelectionParameters =
              player.trackSelectionParameters
                  .buildUpon()
                  .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                  .build()
        } else {
          findTrack(player, C.TRACK_TYPE_TEXT, id)?.let { selection ->
            player.trackSelectionParameters =
                player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(
                        TrackSelectionOverride(selection.group, listOf(selection.index))
                    )
                    .build()
          }
        }
      }
    }
  }

  private fun startStateUpdates() {
    viewModelScope.launch {
      while (isActive) {
        position = player.currentPosition.coerceAtLeast(0L)
        duration = player.duration.takeIf { it > 0L } ?: 0L
        isPlaying = player.isPlaying
        delay(250L.milliseconds)
      }
    }
  }

  private fun startAutoSave() {
    viewModelScope.launch {
      while (isActive) {
        if (player.isPlaying) {
          savePlaybackState()
        }
        delay(5_000L.milliseconds)
      }
    }
  }

  fun savePlaybackState() {
    val client = client ?: return
    val share = shareName ?: return
    val smbFile = file ?: return

    viewModelScope.launch {
      playbackStateStore.save(
          ip = client.server!!.ipAddress,
          share = share,
          path = smbFile.path,
          state =
              PlaybackState(
                  position = player.currentPosition.coerceAtLeast(0L),
                  duration = player.duration.takeIf { it > 0L } ?: 0L,
                  audioTrack = getSelectedTrackId(player, C.TRACK_TYPE_AUDIO),
                  subtitleTrack = getSelectedSubtitleTrackId(player),
              ),
      )
    }
  }

  fun togglePlay() {
    if (player.isPlaying) {
      player.pause()
    } else {
      viewModelScope.launch {
        try {
          client?.ensureConnected()
          player.play()
        } catch (e: Exception) {
          logger.error("Failed to reconnect SMB", e)
        }
      }
    }
  }

  fun downloadAndLoadSubtitle(subtitleId: String, subtitleName: String) {
    val share = shareName ?: return
    val smbFile = file ?: return
    val client = client ?: return

    viewModelScope.launch {
      try {
        val bytes = subtitleServerManager.downloadSubtitle(subtitleId)
        loadExternalSubtitle(bytes, subtitleName, client, share, smbFile)
        showSubtitleMenu = false
      } catch (e: Exception) {
        logger.error("Error during subtitle download or processing", e)
      }
    }
  }

  private suspend fun loadExternalSubtitle(
      bytes: ByteArray,
      name: String,
      client: com.silversky.core.client.SmbClient,
      share: String,
      smbFile: com.silversky.core.smb.SmbEntry,
  ) {
    val currentPosition = player.currentPosition
    val wasPlaying = player.isPlaying

    val videoUri = buildSmbUri(shareName = share, path = smbFile.path)
    val cacheDir = getSubtitleCacheDir(context = context, videoUri = videoUri)
    val subtitleFile = File(cacheDir, name)

    try {
      withContext(Dispatchers.IO) {
        subtitleFile.parentFile?.mkdirs()
        subtitleFile.writeBytes(bytes)
      }
    } catch (e: Exception) {
      logger.error("Failed to save subtitle", e)
      return
    }

    val mediaSource =
        prepareSmbMediaSource(
            context = context,
            smbClient = client,
            shareName = share,
            path = smbFile.path,
            logger = logger,
        )

    player.setMediaSource(mediaSource, currentPosition)
    player.prepare()

    viewModelScope.launch {
      val label = "[Cached] $name"
      var selection: com.silversky.skywatch.ui.TrackSelection? = null
      for (i in 0 until 10) {
        selection = findTrack(player, C.TRACK_TYPE_TEXT, label)
        if (selection != null) break
        delay(100.milliseconds)
      }

      if (selection != null) {
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(
                    TrackSelectionOverride(selection.group, listOf(selection.index))
                )
                .build()
      }
    }

    if (wasPlaying) {
      player.play()
    }
  }

  fun back(onBack: () -> Unit) {
    savePlaybackState()
    player.stop()
    connectionManager.clearFile()
    onBack()
  }

  override fun onCleared() {
    super.onCleared()
    logger.debug("Releasing player")
    savePlaybackState()
    player.stop()
    player.release()
  }
}
