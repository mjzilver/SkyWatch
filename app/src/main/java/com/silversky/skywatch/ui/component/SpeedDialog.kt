package com.silversky.skywatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Text

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

  Dialog(
      onDismissRequest = onDismiss,
      properties =
          DialogProperties(
              dismissOnBackPress = true,
              dismissOnClickOutside = true,
          ),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth(0.55f)
                .background(
                    Color(0xFF202020),
                    RoundedCornerShape(12.dp),
                )
                .padding(32.dp),
    ) {
      Text(
          text = "Playback Speed",
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(20.dp))

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

      Spacer(modifier = Modifier.height(20.dp))

      PlayerButton(
          text = "Close",
          onClick = onDismiss,
      )
    }
  }
}
