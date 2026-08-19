package com.silversky.skywatch.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
suspend fun prepareSmbMediaSource(
    smbClient: SmbClient,
    shareName: String,
    path: String,
    logger: Logger,
): MediaSource {
  return withContext(Dispatchers.IO) {
    val file =
        smbClient.openFile(
            shareName = shareName,
            path = path,
        )

    file ?: throw IllegalStateException("File not found: //$shareName/$path")

    file.close()

    val videoUri =
        buildSmbUri(
            shareName = shareName,
            path = path,
        )

    val directory = path.substringBeforeLast('\\', "")

    val subtitles =
        smbClient
            .list(
                shareName = shareName,
                path = directory,
            )
            .filter { entry ->
              !entry.isDirectory &&
                  entry.name.endsWith(
                      ".srt",
                      ignoreCase = true,
                  )
            }
            .map { subtitle ->
              MediaItem.SubtitleConfiguration.Builder(
                      buildSmbUri(
                              shareName = shareName,
                              path = subtitle.path,
                          )
                          .toUri()
                  )
                  .setMimeType("application/x-subrip")
                  .setLabel(subtitle.name)
                  .build()
            }

    val mediaItem =
        MediaItem.Builder().setUri(videoUri).setSubtitleConfigurations(subtitles).build()

    val dataSourceFactory = SmbDataSourceFactory(smbClient, logger)

    DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
  }
}

@OptIn(UnstableApi::class)
fun createSmbPlayer(context: Context): ExoPlayer {
  val trackSelector = DefaultTrackSelector(context)

  val loadControl =
      DefaultLoadControl.Builder()
          .setBufferDurationsMs(
              5_000, // min buffer
              15_000, // max buffer
              1_500, // playback start
              3_000, // rebuffer start
          )
          .setTargetBufferBytes(32 * 1024 * 1024)
          .setPrioritizeTimeOverSizeThresholds(false)
          .build()

  return ExoPlayer.Builder(context)
      .setTrackSelector(trackSelector)
      .setLoadControl(loadControl)
      .build()
}

private fun buildSmbUri(
    shareName: String,
    path: String,
): String {
  return "smb://$shareName/${
        path.replace('\\', '/').trimStart('/')
    }"
}
