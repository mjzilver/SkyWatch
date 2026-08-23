package com.silversky.skywatch.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.text.Cue
import androidx.tv.material3.Text
import com.silversky.skywatch.player.SubtitleCue

@Composable
fun SubtitleOverlay(
    externalCues: List<SubtitleCue>?,
    internalCues: List<Cue>,
    position: Long,
    offset: Long,
    fontSize: Int,
    fontFamily: String,
    modifier: Modifier = Modifier,
) {
  val textToShow =
      remember(externalCues, internalCues, position, offset) {
        if (!externalCues.isNullOrEmpty()) {
          val adjustedPosition = position + offset
          externalCues
              .find { it.startTime <= adjustedPosition && it.endTime >= adjustedPosition }
              ?.text
        } else if (internalCues.isNotEmpty()) {
          internalCues.joinToString("\n") { it.text ?: "" }
        } else {
          null
        }
      }

  if (!textToShow.isNullOrEmpty()) {
    Box(
        modifier = modifier.fillMaxSize().padding(bottom = 48.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
      Text(
          text = textToShow,
          style =
              TextStyle(
                  color = Color.White,
                  fontSize = fontSize.sp,
                  fontFamily =
                      when (fontFamily) {
                        "Serif" -> FontFamily.Serif
                        "Monospace" -> FontFamily.Monospace
                        else -> FontFamily.SansSerif
                      },
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  shadow =
                      Shadow(
                          color = Color.Black,
                          blurRadius = 8f,
                      ),
              ),
          modifier = Modifier.padding(horizontal = 32.dp),
      )
    }
  }
}
