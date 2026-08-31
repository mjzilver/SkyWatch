package com.silversky.skywatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Text
import com.silversky.skywatch.ui.viewmodel.PlayerViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun DebugInfoDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
) {
  val player = viewModel.player
  val videoFormat = player.videoFormat
  val audioFormat = player.audioFormat

  SkyWatchDialog(
      title = "Playback Information",
      onDismiss = onDismiss,
      modifier = Modifier.fillMaxWidth(0.65f),
      buttons = {
        PlayerButton(
            text = "Close",
            onClick = onDismiss,
        )
      },
  ) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
      LazyColumn(
          state = listState,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize().padding(end = 16.dp),
      ) {
        item {
          DebugSection(title = "Network") {
            DebugItem("Throughput", formatThroughput(viewModel.bandwidthEstimate))
          }
        }

        item {
          DebugSection(title = "Video") {
            videoFormat?.let { format ->
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DebugItem("Resolution", "${format.width}x${format.height}")
                DebugItem("Bitrate", formatBitrate(format.bitrate))
                DebugItem("Codec", format.sampleMimeType ?: "Unknown")
                DebugItem("Codecs", format.codecs ?: "Unknown")
                DebugItem("Frame Rate", "${format.frameRate.toInt()} fps")
                DebugItem("Dropped Frames", "${viewModel.droppedFrames}")
                viewModel.videoDecoderName?.let { DebugItem("Decoder", it) }
              }
            } ?: Text("Video information unavailable", color = Color.Gray, fontSize = 14.sp)
          }
        }

        item {
          DebugSection(title = "Audio") {
            audioFormat?.let { format ->
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DebugItem("Bitrate", formatBitrate(format.bitrate))
                DebugItem("Codec", format.sampleMimeType ?: "Unknown")
                DebugItem("Channels", "${format.channelCount}")
                DebugItem("Sample Rate", "${format.sampleRate} Hz")
                viewModel.audioDecoderName?.let { DebugItem("Decoder", it) }
              }
            } ?: Text("Audio information unavailable", color = Color.Gray, fontSize = 14.sp)
          }
        }
      }

      val showScrollbar by remember {
        derivedStateOf {
          listState.layoutInfo.visibleItemsInfo.size < listState.layoutInfo.totalItemsCount
        }
      }

      if (showScrollbar) {
        Box(
            modifier =
                Modifier.align(Alignment.CenterEnd)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
        ) {
          val scrollRatio by remember {
            derivedStateOf {
              val first = listState.firstVisibleItemIndex
              val total = listState.layoutInfo.totalItemsCount
              if (total > 0) first.toFloat() / total else 0f
            }
          }

          val heightRatio by remember {
            derivedStateOf {
              val visible = listState.layoutInfo.visibleItemsInfo.size
              val total = listState.layoutInfo.totalItemsCount
              if (total > 0) visible.toFloat() / total else 1f
            }
          }

          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .fillMaxHeight(heightRatio)
                      .align(Alignment.TopCenter)
                      .offset { IntOffset(0, (400.dp.toPx() * scrollRatio).toInt()) }
                      .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
          )
        }
      }
    }
  }
}

@Composable
private fun DebugSection(
    title: String,
    content: @Composable () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
  ) {
    Text(
        text = title,
        color = Color(0xFFFFCC00),
        fontSize = 16.sp,
    )
    Spacer(modifier = Modifier.height(8.dp))
    content()
    Spacer(modifier = Modifier.height(8.dp))
  }
}

@Composable
private fun DebugItem(
    label: String,
    value: String,
) {
  Row(
      modifier = Modifier.fillMaxWidth().focusable(),
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(text = label, color = Color.LightGray, fontSize = 14.sp)
    Text(text = value, color = Color.White, fontSize = 14.sp)
  }
}

private fun formatThroughput(bps: Long): String {
  if (bps <= 0) return "Measuring..."
  val mbps = bps / 1_000_000.0
  return "%.2f Mbps".format(mbps)
}

private fun formatBitrate(bps: Int): String {
  if (bps <= 0) return "Unknown"
  val mbps = bps / 1_000_000.0
  return "%.2f Mbps".format(mbps)
}
