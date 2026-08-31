package com.silversky.skywatch.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import com.silversky.skywatch.data.local.PlaybackState

enum class PlaybackStatus {
  NotStarted,
  InProgress,
  Finished,
}

fun getPlaybackStatus(state: PlaybackState?): PlaybackStatus {
  if (state == null) return PlaybackStatus.NotStarted
  return if (state.completed || (state.duration > 0 && state.position >= state.duration * 0.90)) {
    PlaybackStatus.Finished
  } else if (state.position > 0) {
    PlaybackStatus.InProgress
  } else {
    PlaybackStatus.NotStarted
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusIcon(
    status: PlaybackStatus,
    modifier: Modifier = Modifier,
) {
  Icon(
      imageVector =
          when (status) {
            PlaybackStatus.Finished -> Icons.Filled.Done
            PlaybackStatus.InProgress -> Icons.Filled.PlayCircle
            PlaybackStatus.NotStarted -> Icons.Filled.PlayArrow
          },
      contentDescription = null,
      modifier = modifier,
  )
}
