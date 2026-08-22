package com.silversky.skywatch.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun getSubtitleCacheDir(context: Context, videoUri: String): File {
  val hash = videoUri.hashCode().toString()
  val dir = File(context.cacheDir, "subtitles/$hash")
  if (!dir.exists()) dir.mkdirs()
  return dir
}

@OptIn(UnstableApi::class)
suspend fun prepareSmbMediaItem(
    context: Context,
    smbClient: SmbClient,
    shareName: String,
    path: String,
    logger: Logger,
): MediaItem {
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

    val smbSubtitles =
        try {
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
                val label = subtitle.name
                MediaItem.SubtitleConfiguration.Builder(
                        buildSmbUri(
                                shareName = shareName,
                                path = subtitle.path,
                            )
                            .toUri()
                    )
                    .setMimeType("application/x-subrip")
                    .setLabel(label)
                    .setId(label)
                    .build()
              }
        } catch (e: Exception) {
          logger.error("Failed to list local subtitles", e)
          emptyList()
        }

    val cachedSubtitles =
        try {
          val cacheDir = getSubtitleCacheDir(context, videoUri)
          cacheDir
              .listFiles { file -> file.extension.lowercase() == "srt" }
              ?.map { file ->
                val label = "[Cached] ${file.name}"
                MediaItem.SubtitleConfiguration.Builder(file.toUri())
                    .setMimeType("application/x-subrip")
                    .setLabel(label)
                    .setId(label)
                    .build()
              } ?: emptyList()
        } catch (e: Exception) {
          logger.error("Failed to list cached subtitles", e)
          emptyList()
        }

    MediaItem.Builder()
        .setUri(videoUri)
        .setSubtitleConfigurations(smbSubtitles + cachedSubtitles)
        .build()
  }
}

@OptIn(UnstableApi::class)
fun createSmbPlayer(
    context: Context,
    smbClient: SmbClient,
    logger: Logger,
): ExoPlayer {
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

  val smbDataSourceFactory = SmbDataSourceFactory(smbClient, logger)
  val dataSourceFactory =
      androidx.media3.datasource.DefaultDataSource.Factory(
          context,
          smbDataSourceFactory,
      )

  val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

  return ExoPlayer.Builder(context)
      .setTrackSelector(trackSelector)
      .setLoadControl(loadControl)
      .setMediaSourceFactory(mediaSourceFactory)
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
