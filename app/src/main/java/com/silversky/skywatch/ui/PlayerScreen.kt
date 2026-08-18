package com.silversky.skywatch.ui

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.skywatch.media.createSmbPlayer

@Composable
fun PlayerScreen(
    client: SmbClient, shareName: String, file: SmbEntry, logger: Logger, onBack: () -> Unit
) {
    val context = LocalContext.current

    val player = remember(
        shareName, file.path
    ) {
        logger.info(
            "Starting playback: //$shareName/${file.path}"
        )

        createSmbPlayer(
            context = context, smbClient = client, shareName = shareName, path = file.path
        )
    }

    BackHandler {
        player.stop()
        onBack()
    }

    DisposableEffect(player) {
        onDispose {
            logger.debug(
                "Releasing player: ${file.name}"
            )

            player.release()
        }
    }

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
        PlayerView(context).apply {
            this.player = player

            useController = true

            controllerShowTimeoutMs = 3_000

            setShowBuffering(
                PlayerView.SHOW_BUFFERING_WHEN_PLAYING
            )

            keepScreenOn = true

            focusable = View.FOCUSABLE
            isFocusableInTouchMode = true
        }
    }, update = { view ->
        view.player = player
    })
}