package com.silversky.skywatch.player

data class SubtitleCue(
    val startTime: Long,
    val endTime: Long,
    val text: String,
)

object SubtitleParser {
  private val TIME_PATTERN =
      Regex(
          """(\d{1,2}):(\d{1,2}):(\d{1,2})[.,](\d{1,3})\s*-->\s*(\d{1,2}):(\d{1,2}):(\d{1,2})[.,](\d{1,3})"""
      )

  fun parseSrt(content: String): List<SubtitleCue> {
    val cues = mutableListOf<SubtitleCue>()
    val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
    val blocks = normalized.split(Regex("\\n\\s*\\n"))

    for (block in blocks) {
      val lines = block.trim().lines()
      if (lines.size < 2) continue

      val timeLineIndex = lines.indexOfFirst { TIME_PATTERN.containsMatchIn(it) }
      if (timeLineIndex == -1) continue

      val match = TIME_PATTERN.find(lines[timeLineIndex]) ?: continue

      val startTime =
          parseTime(
              match.groupValues[1],
              match.groupValues[2],
              match.groupValues[3],
              match.groupValues[4],
          )

      val endTime =
          parseTime(
              match.groupValues[5],
              match.groupValues[6],
              match.groupValues[7],
              match.groupValues[8],
          )

      val textLines = lines.drop(timeLineIndex + 1)
      if (textLines.isEmpty()) continue

      cues +=
          SubtitleCue(
              startTime = startTime,
              endTime = endTime,
              text = textLines.joinToString("\n").replace(Regex("<[^>]*>"), ""),
          )
    }

    return cues
  }

  private fun parseTime(
      h: String,
      m: String,
      s: String,
      ms: String,
  ): Long {
    // Pad milliseconds to 3 digits if necessary
    val msLong =
        when (ms.length) {
          1 -> ms.toLong() * 100
          2 -> ms.toLong() * 10
          else -> ms.toLong()
        }
    return (h.toLong() * 3_600_000 + m.toLong() * 60_000 + s.toLong() * 1_000 + msLong)
  }
}
