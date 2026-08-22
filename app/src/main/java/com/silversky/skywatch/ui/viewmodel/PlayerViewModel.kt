package com.silversky.skywatch.ui.viewmodel

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
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.silversky.core.logger.Logger
import com.silversky.skywatch.data.local.PlaybackState
import com.silversky.skywatch.data.local.PlaybackStateStore
import com.silversky.skywatch.data.remote.SmbConnectionManager
import com.silversky.skywatch.data.repository.SubtitleRepository
import com.silversky.skywatch.player.createSmbPlayer
import com.silversky.skywatch.player.getSubtitleCacheDir
import com.silversky.skywatch.player.prepareSmbMediaItem
import com.silversky.skywatch.ui.component.findTrack
import com.silversky.skywatch.ui.component.getSelectedSubtitleTrackId
import com.silversky.skywatch.ui.component.getSelectedTrackId
import com.silversky.skywatch.utils.buildSmbUri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@HiltViewModel
class PlayerViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val connectionManager: SmbConnectionManager,
    val playbackStateStore: PlaybackStateStore,
    val subtitleRepository: SubtitleRepository,
    private val logger: Logger,
) : ViewModel() {

  val player: ExoPlayer =
      createSmbPlayer(
          context,
          connectionManager.smbClient!!,
          logger,
      )

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

          override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@PlayerViewModel.isPlaying = isPlaying
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

        val mediaItem =
            prepareSmbMediaItem(
                context = context,
                smbClient = client,
                shareName = share,
                path = smbFile.path,
                logger = logger,
            )

        player.setMediaItem(mediaItem)
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

    viewModelScope.launch {
      try {
        val bytes = subtitleRepository.downloadSubtitle(subtitleId)
        loadExternalSubtitle(bytes, subtitleName, share, smbFile)
      } catch (e: Exception) {
        logger.error("Error during subtitle download or processing", e)
      }
    }
  }

  private suspend fun loadExternalSubtitle(
      bytes: ByteArray,
      name: String,
      share: String,
      smbFile: com.silversky.core.smb.SmbEntry,
  ) {
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

    val currentMediaItem = player.currentMediaItem ?: return
    val label = "[Cached] $name"

    val newSubtitleConfig =
        androidx.media3.common.MediaItem.SubtitleConfiguration.Builder(
                android.net.Uri.fromFile(subtitleFile)
            )
            .setMimeType("application/x-subrip")
            .setLabel(label)
            .setId(label)
            .build()

    val currentConfigs = currentMediaItem.localConfiguration?.subtitleConfigurations ?: emptyList()
    val newConfigs = currentConfigs.filter { it.id != label }.toMutableList()
    newConfigs.add(newSubtitleConfig)

    val newMediaItem = currentMediaItem.buildUpon().setSubtitleConfigurations(newConfigs).build()

    player.setMediaItem(newMediaItem, false)
    player.prepare()

    player.addListener(
        object : Player.Listener {
          override fun onTracksChanged(tracks: Tracks) {
            val selection = findTrack(player, C.TRACK_TYPE_TEXT, label)
            if (selection != null) {
              player.trackSelectionParameters =
                  player.trackSelectionParameters
                      .buildUpon()
                      .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                      .setOverrideForType(
                          TrackSelectionOverride(selection.group, listOf(selection.index))
                      )
                      .build()
              showSubtitleMenu = false
              player.removeListener(this)
            }
          }

          override fun onPlayerError(error: PlaybackException) {
            player.removeListener(this)
          }
        }
    )

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
    logger.debug("Releasing player")
    savePlaybackState()
    player.stop()
    player.release()
  }
}
