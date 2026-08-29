package com.silversky.skywatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer

@Composable
internal fun SpeedDialog(
    player: ExoPlayer,
    onDismiss: () -> Unit,
) {
  val speeds =
      listOf(
          0.5f,
          0.75f,
          1.0f,
          1.25f,
          1.5f,
          2.0f,
      )

  SkyWatchDialog(
      title = "Playback Speed",
      onDismiss = onDismiss,
      modifier = Modifier.fillMaxWidth(0.55f),
      buttons = {
        PlayerButton(
            text = "Close",
            onClick = onDismiss,
        )
      },
  ) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(speeds) { speed ->
        TrackButton(
            text = "${speed}x",
            selected = player.playbackParameters.speed == speed,
            onClick = {
              player.setPlaybackSpeed(speed)
              onDismiss()
            },
        )
      }
    }
  }
}
