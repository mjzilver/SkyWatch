package com.silversky.skywatch.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format.NO_VALUE
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.silversky.core.logger.Logger
import com.silversky.skywatch.data.local.PlaybackState
import com.silversky.skywatch.data.local.PlaybackStateStore
import com.silversky.skywatch.data.local.SubtitleStore
import com.silversky.skywatch.data.remote.SmbConnectionManager
import com.silversky.skywatch.data.repository.SettingsRepository
import com.silversky.skywatch.data.repository.SubtitleRepository
import com.silversky.skywatch.error.AppErrorEvent
import com.silversky.skywatch.error.AppErrorReporter
import com.silversky.skywatch.player.SubtitleCue
import com.silversky.skywatch.player.SubtitleParser
import com.silversky.skywatch.player.createSmbPlayer
import com.silversky.skywatch.player.prepareSmbMediaItem
import com.silversky.skywatch.ui.component.findTrack
import com.silversky.skywatch.ui.component.getSelectedSubtitleTrackId
import com.silversky.skywatch.ui.component.getSelectedTrackId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@HiltViewModel
class PlayerViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val connectionManager: SmbConnectionManager,
    val playbackStateStore: PlaybackStateStore,
    val subtitleRepository: SubtitleRepository,
    val subtitleStorage: SubtitleStore,
    val settingsRepository: SettingsRepository,
    private val logger: Logger,
) : ViewModel() {

  val player: ExoPlayer =
      createSmbPlayer(
          context,
          connectionManager.smbClient,
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

  var showDebugMenu by mutableStateOf(false)

  var onPlaybackEnded: (() -> Unit)? = null

  var position by mutableLongStateOf(0L)
    private set

  var duration by mutableLongStateOf(0L)
    private set

  var isPlaying by mutableStateOf(false)
    private set

  var subtitleOffset by mutableLongStateOf(0L)
    private set

  var externalSubtitles by mutableStateOf<List<SubtitleCue>?>(null)
    private set

  var externalSubtitleName by mutableStateOf<String?>(null)
    private set

  var internalCues by mutableStateOf<List<Cue>>(emptyList())
    private set

  var bandwidthEstimate by mutableLongStateOf(0L)
    private set

  private val bandwidthSamples = mutableListOf<Long>()
  private val maxSamples = 5

  var videoBitrate by mutableIntStateOf(0)
    private set

  var showLowBandwidthWarning by mutableStateOf(false)
    private set

  var videoDecoderName by mutableStateOf<String?>(null)
    private set

  var audioDecoderName by mutableStateOf<String?>(null)
    private set

  var droppedFrames by mutableIntStateOf(0)
    private set

  private var savedState: PlaybackState? = null
  private var tracksApplied = false
  private var saveJob: Job? = null

  val client
    get() = connectionManager.smbClient

  val shareName
    get() = connectionManager.selectedShare

  val file
    get() = connectionManager.selectedFile

  init {
    if (connectionManager.smbClient == null) {
      loading = false
      error = "Connection to server was lost."
    } else {
      setupPlayer()
      startPlayback()
      startStateUpdates()
      startAutoSave()
    }
  }

  private fun setupPlayer() {
    player.addAnalyticsListener(
        object : AnalyticsListener {
          override fun onBandwidthEstimate(
              eventTime: AnalyticsListener.EventTime,
              totalLoadTimeMs: Int,
              totalBytesLoaded: Long,
              bitrateEstimate: Long,
          ) {
            if (bitrateEstimate > 0) {
              addBandwidthSample(bitrateEstimate)
            }
          }

          override fun onLoadCompleted(
              eventTime: AnalyticsListener.EventTime,
              loadEventInfo: LoadEventInfo,
              mediaLoadData: MediaLoadData,
          ) {
            val bytes = loadEventInfo.bytesLoaded
            val durationMs = loadEventInfo.loadDurationMs
            if (bytes > 0 && durationMs > 0) {
              val currentBitrate = (bytes * 8000) / durationMs
              if (currentBitrate > 0) {
                addBandwidthSample(currentBitrate)
              }
            }
          }

          override fun onVideoDecoderInitialized(
              eventTime: AnalyticsListener.EventTime,
              decoderName: String,
              initializedTimestampMs: Long,
              initializationDurationMs: Long,
          ) {
            videoDecoderName = decoderName
          }

          override fun onAudioDecoderInitialized(
              eventTime: AnalyticsListener.EventTime,
              decoderName: String,
              initializedTimestampMs: Long,
              initializationDurationMs: Long,
          ) {
            audioDecoderName = decoderName
          }
        }
    )

    player.addListener(
        object : Player.Listener {
          override fun onPlaybackStateChanged(state: Int) {
            logger.debug(
                "PLAYER STATE: ${
                            when (state) {
                                Player.STATE_IDLE -> "IDLE"
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_READY -> "READY"
                                Player.STATE_ENDED -> {
                                    savePlaybackState(isFinished = true)
                                    onPlaybackEnded?.invoke()
                                    "ENDED"
                                }
                                else -> "UNKNOWN"
                            }
                        }"
            )
          }

          override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@PlayerViewModel.isPlaying = isPlaying
          }

          override fun onTracksChanged(tracks: Tracks) {
            updateVideoBitrate()
            if (!tracksApplied && !tracks.groups.isEmpty()) {
              applySavedTracks()
            }
          }

          override fun onMediaItemTransition(
              mediaItem: MediaItem?,
              reason: Int,
          ) {
            tracksApplied = false
          }

          override fun onCues(cueGroup: CueGroup) {
            internalCues = cueGroup.cues
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

  private fun addBandwidthSample(sample: Long) {
    bandwidthSamples.add(sample)
    if (bandwidthSamples.size > maxSamples) {
      bandwidthSamples.removeAt(0)
    }
    bandwidthEstimate = bandwidthSamples.average().toLong()
    updateBandwidthWarning()
  }

  private fun updateVideoBitrate() {
    val format = player.videoFormat
    if (format != null && format.bitrate != NO_VALUE) {
      videoBitrate = format.bitrate
    }
  }

  private fun updateBandwidthWarning() {
    val bitrate = videoBitrate
    val estimate = bandwidthEstimate

    showLowBandwidthWarning =
        if (bitrate > 0 && estimate > 0 && player.playbackState == Player.STATE_READY) {
          estimate < (bitrate * 1.2).toLong()
        } else {
          false
        }
  }

  private fun startPlayback() {
    val share = shareName ?: return
    val smbFile = file ?: return
    val client = client ?: return

    viewModelScope.launch {
      loading = true
      error = null

      try {
        savedState =
            playbackStateStore.get(
                client.server!!.ipAddress,
                share.name,
                smbFile.path,
            )

        subtitleOffset = savedState?.subtitleOffset ?: 0L

        val mediaItem =
            prepareSmbMediaItem(
                context = context,
                smbClient = client,
                shareName = share.name,
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

        player.playWhenReady = true
        loading = false
      } catch (e: Exception) {
        logger.error("Failed to start playback: ${smbFile.name}", e)
        if (isClosedShareError(e)) {
          connectionManager.clearFile()
          connectionManager.clearShare()
          AppErrorReporter.report(
              AppErrorEvent.ConnectionLost(
                  client.server?.name ?: client.server?.ipAddress ?: "server"
              )
          )
          return@launch
        }

        loading = false
        error = e.message ?: "Failed to start playback"
      }
    }
  }

  private fun applySavedTracks() {
    val state = savedState ?: return
    val smbFile = file ?: return
    if (player.currentTracks.groups.isEmpty()) return

    tracksApplied = true

    viewModelScope.launch {
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
          val cached = subtitleStorage.getCachedSubtitles(smbFile.name)
          val cachedMatch = cached.find { it.name == id }
          if (cachedMatch != null) {
            loadCachedSubtitle(cachedMatch.name, cachedMatch.content)
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
  }

  private fun startStateUpdates() {
    viewModelScope.launch {
      while (isActive) {
        position = player.currentPosition.coerceAtLeast(0L)
        duration = player.duration.takeIf { it > 0L } ?: 0L

        if (showDebugMenu) {
          droppedFrames = player.videoDecoderCounters?.droppedBufferCount ?: 0
          // Force bitrate/bandwidth update if needed
          updateVideoBitrate()
        }

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

  fun savePlaybackState(isFinished: Boolean = false) {
    val client = client ?: return
    val share = shareName ?: return
    val smbFile = file ?: return

    saveJob?.cancel()
    saveJob = viewModelScope.launch {
      val currentPos = player.currentPosition.coerceAtLeast(0L)
      val totalDuration = player.duration.takeIf { it > 0L } ?: 0L

      val percent = if (totalDuration > 0) currentPos.toDouble() / totalDuration else 0.0

      val isCompleted = isFinished || percent >= 0.90
      val shouldReset = isFinished || percent > 0.99

      val finalPosition = if (shouldReset) 0L else currentPos

      val subTrack =
          if (externalSubtitleName != null) {
            externalSubtitleName
          } else {
            getSelectedSubtitleTrackId(player)
          }

      playbackStateStore.save(
          ip = client.server!!.ipAddress,
          share = share.name,
          path = smbFile.path,
          state =
              PlaybackState(
                  position = finalPosition,
                  duration = totalDuration,
                  audioTrack = getSelectedTrackId(player, C.TRACK_TYPE_AUDIO),
                  subtitleTrack = subTrack,
                  completed = isCompleted,
                  subtitleOffset = subtitleOffset,
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
    viewModelScope.launch {
      try {
        val bytes = subtitleRepository.downloadSubtitle(subtitleId)
        val content = String(bytes, Charsets.UTF_8)
        val parsed =
            withContext(Dispatchers.Default) {
              SubtitleParser.parse(content, logger)
            }
        externalSubtitles = parsed
        externalSubtitleName = subtitleName

        file?.let {
          subtitleStorage.saveSubtitle(it.name, subtitleName, content)
        }

        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        showSubtitleMenu = false
      } catch (e: Exception) {
        logger.error("Error during subtitle download or processing", e)
      }
    }
  }

  fun loadCachedSubtitle(subtitleName: String, content: String) {
    viewModelScope.launch {
      val parsed =
          withContext(Dispatchers.Default) {
            SubtitleParser.parse(content, logger)
          }
      externalSubtitles = parsed
      externalSubtitleName = subtitleName
      player.trackSelectionParameters =
          player.trackSelectionParameters
              .buildUpon()
              .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
              .build()
    }
  }

  fun updateSubtitleOffset(offset: Long) {
    subtitleOffset = offset
  }

  fun clearExternalSubtitles() {
    externalSubtitles = null
    externalSubtitleName = null
  }

  private fun isClosedShareError(throwable: Throwable): Boolean {
    val message = throwable.message ?: return false
    val text = throwable.toString() + " " + message

    return text.contains("DiskShare has already been closed", ignoreCase = true) ||
        text.contains("Share has already been closed", ignoreCase = true) ||
        text.contains("connection closed", ignoreCase = true) ||
        text.contains("Timeout expired", ignoreCase = true) ||
        text.contains("TimeoutException", ignoreCase = true) ||
        text.contains("TransportException", ignoreCase = true)
  }

  fun back(onBack: () -> Unit) {
    savePlaybackState()
    player.stop()
    connectionManager.clearFile()
    onBack()
  }

  override fun onCleared() {
    savePlaybackState()
    player.stop()
    player.release()
  }
}
