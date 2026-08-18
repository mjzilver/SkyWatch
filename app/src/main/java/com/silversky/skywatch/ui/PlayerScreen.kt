package com.silversky.skywatch.ui

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.skywatch.media.createSmbPlayer
import com.silversky.skywatch.media.prepareSmbMediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    client: SmbClient,
    shareName: String,
    file: SmbEntry,
    logger: Logger,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var player by remember {
        mutableStateOf<ExoPlayer?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val exoPlayer = remember {
        createSmbPlayer(context)
    }

    LaunchedEffect(
        shareName,
        file.path
    ) {
        loading = true
        error = null

        try {
            logger.info(
                "Starting playback: //$shareName/${file.path}"
            )

            val mediaSource = withContext(Dispatchers.IO) {
                prepareSmbMediaSource(
                    smbClient = client,
                    shareName = shareName,
                    path = file.path
                )
            }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            player = exoPlayer
            loading = false
        } catch (e: Exception) {
            logger.error(
                "Failed to start playback: ${file.name}",
                e
            )

            loading = false
            error = e.message ?: "Failed to play file"
        }
    }

    BackHandler {
        exoPlayer.stop()
        onBack()
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            logger.debug(
                "Releasing player: ${file.name}"
            )

            exoPlayer.release()
        }
    }

    when {
        loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        }

        error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error!!
                )
            }
        }

        else -> {
            AndroidView(
                modifier = Modifier.fillMaxSize(),

                factory = { context ->
                    PlayerView(context).apply {
                        this.player = exoPlayer

                        useController = true

                        controllerShowTimeoutMs = 3_000

                        setShowBuffering(
                            PlayerView.SHOW_BUFFERING_WHEN_PLAYING
                        )

                        keepScreenOn = true

                        focusable = View.FOCUSABLE
                        isFocusableInTouchMode = true
                    }
                },

                update = { view ->
                    view.player = exoPlayer
                }
            )
        }
    }
}