package com.silversky.skywatch.player

import com.silversky.core.logger.Logger

object SrtParser : SubtitleFormatParser {
  override fun canParse(content: String): Boolean {
    return content
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(20)
        .any { parseTimestampLine(it) != null }
  }

  override fun parse(content: String, logger: Logger?): List<SubtitleCue> {
    val cues = mutableListOf<SubtitleCue>()
    if (content.isBlank()) return cues

    val lines = content.replace("\r\n", "\n").replace("\r", "\n").split('\n')
    var i = 0
    while (i < lines.size) {
      val line = lines[i].trim()
      if (line.isEmpty()) {
        i++
        continue
      }

      if (line.contains(" --> ")) {
        val timestamps = parseTimestampLine(line)
        if (timestamps != null) {
          val (start, end) = timestamps
          val textBuilder = StringBuilder()
          i++
          while (i < lines.size) {
            val textLine = lines[i]
            val trimmed = textLine.trim()
            if (trimmed.isEmpty()) break
            if (trimmed.contains(" --> ") && parseTimestampLine(trimmed) != null) {
              i--
              break
            }
            if (textBuilder.isNotEmpty()) textBuilder.append('\n')
            textBuilder.append(textLine)
            i++
          }
          val text = textBuilder.toString()
          if (text.isNotBlank()) {
            cues.add(SubtitleCue(start, end, text))
          }
        }
      }
      i++
    }
    return cues
  }

  private fun parseTimestampLine(line: String): Pair<Long, Long>? {
    val arrow = " --> "
    val arrowIndex = line.indexOf(arrow)
    if (arrowIndex == -1) return null

    val startPart = line.substring(0, arrowIndex).trim()
    val endPartFull = line.substring(arrowIndex + arrow.length).trim()

    var firstSpace = -1
    for (j in endPartFull.indices) {
      if (endPartFull[j].isWhitespace()) {
        firstSpace = j
        break
      }
    }
    val endPart = if (firstSpace == -1) endPartFull else endPartFull.substring(0, firstSpace)

    val start = parseSingleTime(startPart) ?: return null
    val end = parseSingleTime(endPart) ?: return null

    return start to end
  }

  private fun parseSingleTime(timeStr: String): Long? {
    val colon1 = timeStr.indexOf(':')
    if (colon1 == -1) return null
    val colon2 = timeStr.indexOf(':', colon1 + 1)
    if (colon2 == -1) return null

    val hours = timeStr.substring(0, colon1).trim().toLongOrNull() ?: return null
    val minutes = timeStr.substring(colon1 + 1, colon2).trim().toLongOrNull() ?: return null

    val remainder = timeStr.substring(colon2 + 1).trim()
    if (remainder.isEmpty()) return null

    var sepIndex = remainder.indexOf(',')
    if (sepIndex == -1) sepIndex = remainder.indexOf('.')

    val seconds: Long
    val millis: Long

    if (sepIndex == -1) {
      var nonDigitIndex = -1
      for (j in remainder.indices) {
        if (!remainder[j].isDigit()) {
          nonDigitIndex = j
          break
        }
      }
      val sStr = if (nonDigitIndex == -1) remainder else remainder.substring(0, nonDigitIndex)
      seconds = sStr.toLongOrNull() ?: return null
      millis = 0L
    } else {
      seconds = remainder.substring(0, sepIndex).toLongOrNull() ?: return null
      val msPart = remainder.substring(sepIndex + 1)
      var nonDigitIndex = -1
      for (j in msPart.indices) {
        if (!msPart[j].isDigit()) {
          nonDigitIndex = j
          break
        }
      }
      val msStr = if (nonDigitIndex == -1) msPart else msPart.substring(0, nonDigitIndex)
      millis =
          when {
            msStr.isEmpty() -> 0L
            msStr.length >= 3 -> msStr.substring(0, 3).toLongOrNull() ?: 0L
            msStr.length == 2 -> (msStr.toLongOrNull() ?: 0L) * 10
            msStr.length == 1 -> (msStr.toLongOrNull() ?: 0L) * 100
            else -> 0L
          }
    }
    return hours * 3600_000 + minutes * 60_000 + seconds * 1000 + millis
  }
}
