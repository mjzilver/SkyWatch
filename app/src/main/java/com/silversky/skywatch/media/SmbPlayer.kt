package com.silversky.skywatch.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.silversky.core.client.SmbClient

fun createSmbPlayer(
    context: Context, smbClient: SmbClient, shareName: String, path: String
): ExoPlayer {

    val dataSourceFactory = SmbDataSourceFactory(smbClient)

    val uri = buildSmbUri(
        shareName = shareName, path = path
    )

    val mediaSource = ProgressiveMediaSource.Factory(
        dataSourceFactory
    ).createMediaSource(
        MediaItem.fromUri(uri)
    )

    return ExoPlayer.Builder(context).build().apply {
        setMediaSource(mediaSource)
        prepare()
        playWhenReady = true
    }
}

private fun buildSmbUri(
    shareName: String, path: String
): String {
    return "smb://$shareName/${
        path.replace('\\', '/').trimStart('/')
    }"
}