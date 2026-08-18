package com.silversky.skywatch.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.silversky.core.client.SmbClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun prepareSmbMediaSource(
    smbClient: SmbClient,
    shareName: String,
    path: String,
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
                      Uri.parse(
                          buildSmbUri(
                              shareName = shareName,
                              path = subtitle.path,
                          )
                      )
                  )
                  .setMimeType("application/x-subrip")
                  .setLabel(subtitle.name)
                  .build()
            }

    val mediaItem =
        MediaItem.Builder().setUri(videoUri).setSubtitleConfigurations(subtitles).build()

    val dataSourceFactory = SmbDataSourceFactory(smbClient)

    DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
  }
}

fun createSmbPlayer(context: Context): ExoPlayer {
  val trackSelector = DefaultTrackSelector(context)

  return ExoPlayer.Builder(context).setTrackSelector(trackSelector).build()
}

private fun buildSmbUri(
    shareName: String,
    path: String,
): String {
  return "smb://$shareName/${
        path.replace('\\', '/').trimStart('/')
    }"
}
