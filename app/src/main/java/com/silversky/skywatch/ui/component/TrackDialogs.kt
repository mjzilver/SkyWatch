package com.silversky.skywatch.ui.component

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

internal data class TrackSelection(
    val group: TrackGroup,
    val index: Int,
)

@Composable
internal fun AudioTrackDialog(
    player: ExoPlayer,
    onDismiss: () -> Unit,
) {
  val tracks =
      remember(player.currentTracks) {
        player.currentTracks.groups.filter {
          it.type == C.TRACK_TYPE_AUDIO
        }
      }

  TrackDialog(
      title = "Audio",
      tracks = tracks,
      player = player,
      trackType = C.TRACK_TYPE_AUDIO,
      allowOff = false,
      onDismiss = onDismiss,
  )
}

@OptIn(UnstableApi::class)
@Composable
internal fun TrackDialog(
    title: String,
    tracks: List<Tracks.Group>,
    player: ExoPlayer,
    trackType: Int,
    allowOff: Boolean,
    onDismiss: () -> Unit,
) {
  SkyWatchDialog(
      title = title,
      onDismiss = onDismiss,
      modifier = Modifier.fillMaxWidth(0.65f),
      buttons = {
        PlayerButton(
            text = "Close",
            onClick = onDismiss,
        )
      },
  ) {
    if (tracks.isEmpty()) {
      Text(
          text = "No $title tracks available.",
          color = Color.LightGray,
      )
    } else {
      LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (allowOff) {
          item {
            TrackButton(
                text = "Off",
                selected = !player.currentTracks.isTypeSelected(trackType),
                onClick = {
                  player.trackSelectionParameters =
                      player.trackSelectionParameters
                          .buildUpon()
                          .setTrackTypeDisabled(
                              trackType,
                              true,
                          )
                          .build()

                  onDismiss()
                },
            )
          }
        }

        items(tracks.indices.toList()) { groupIndex ->
          val group = tracks[groupIndex]

          for (trackIndex in 0 until group.length) {
            val format = group.getTrackFormat(trackIndex)

            val label = format.label ?: format.language ?: "Track ${trackIndex + 1}"

            val selected = group.isTrackSelected(trackIndex)

            TrackButton(
                text = label,
                selected = selected,
                onClick = {
                  player.trackSelectionParameters =
                      player.trackSelectionParameters
                          .buildUpon()
                          .setTrackTypeDisabled(
                              trackType,
                              false,
                          )
                          .setOverrideForType(
                              TrackSelectionOverride(
                                  group.mediaTrackGroup,
                                  listOf(trackIndex),
                              )
                          )
                          .build()

                  onDismiss()
                },
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun TrackButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
      if (selected) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
      } else {
        Spacer(modifier = Modifier.size(18.dp))
      }

      Spacer(modifier = Modifier.width(12.dp))

      Text(text = text)
    }
  }
}

internal fun trackId(
    format: Format,
): String {
  return format.id ?: "${format.language}|${format.label}|${format.sampleMimeType}"
}

@OptIn(UnstableApi::class)
internal fun findTrack(
    player: ExoPlayer,
    trackType: Int,
    id: String?,
): TrackSelection? {
  if (id == null) {
    return null
  }

  for (group in player.currentTracks.groups) {
    if (group.type != trackType) {
      continue
    }

    for (trackIndex in 0 until group.length) {
      val format = group.getTrackFormat(trackIndex)
      val currentId = trackId(format)

      if (currentId == id || currentId.endsWith(":$id") || format.label == id) {
        return TrackSelection(
            group = group.mediaTrackGroup,
            index = trackIndex,
        )
      }
    }
  }

  return null
}

internal fun getSelectedTrackId(
    player: ExoPlayer,
    trackType: Int,
): String? {
  for (group in player.currentTracks.groups) {
    if (group.type != trackType) {
      continue
    }

    for (trackIndex in 0 until group.length) {
      if (group.isTrackSelected(trackIndex)) {
        return trackId(group.getTrackFormat(trackIndex))
      }
    }
  }

  return null
}

internal fun getSelectedSubtitleTrackId(
    player: ExoPlayer,
): String? {
  if (!player.currentTracks.isTypeSelected(C.TRACK_TYPE_TEXT)) {
    return "off"
  }

  return getSelectedTrackId(
      player,
      C.TRACK_TYPE_TEXT,
  )
}
