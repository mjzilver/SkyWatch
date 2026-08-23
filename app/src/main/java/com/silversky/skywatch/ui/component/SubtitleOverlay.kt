package com.silversky.skywatch.ui.component

import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
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
          val cue = externalCues.find {
            it.startTime <= adjustedPosition && it.endTime >= adjustedPosition
          }

          cue?.text?.let { htmlToAnnotatedString(it) }
        } else if (internalCues.isNotEmpty()) {
          val internalText = internalCues.joinToString("\n") { it.text ?: "" }
          htmlToAnnotatedString(internalText)
        } else {
          null
        }
      }

  if (textToShow != null) {
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

private fun htmlToAnnotatedString(text: String): AnnotatedString {
  val spanned = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
  return buildAnnotatedString {
    append(spanned.toString())
    spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
      val start = spanned.getSpanStart(span)
      val end = spanned.getSpanEnd(span)
      when (span) {
        is StyleSpan -> {
          when (span.style) {
            android.graphics.Typeface.BOLD ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            android.graphics.Typeface.ITALIC ->
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            android.graphics.Typeface.BOLD_ITALIC ->
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    start,
                    end,
                )
          }
        }
        is UnderlineSpan ->
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
        is StrikethroughSpan ->
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
        is ForegroundColorSpan ->
            addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
        is RelativeSizeSpan ->
            addStyle(
                SpanStyle(fontSize = 1.sp * span.sizeChange),
                start,
                end,
            ) // Basic relative size support
      }
    }
  }
}
