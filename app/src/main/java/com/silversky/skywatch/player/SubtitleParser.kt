package com.silversky.skywatch.player

data class SubtitleCue(
    val startTime: Long,
    val endTime: Long,
    val text: String,
)

object SubtitleParser {
  private val TIME_PATTERN =
      Regex("""\s*(\d+):(\d+):(\d+)(?:[.,](\d+))?\s*-->\s*(\d+):(\d+):(\d+)(?:[.,](\d+))?.*""")

  fun parseSrt(content: String): List<SubtitleCue> {
    val cues = mutableListOf<SubtitleCue>()
    val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
    // Split by at least two newlines to separate blocks, but allow more
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

      var text = textLines.joinToString("\n")

      // Strip HTML tags
      text = text.replace(Regex("<[^>]*>"), "")

      if (text.isNotBlank()) {
        cues +=
            SubtitleCue(
                startTime = startTime,
                endTime = endTime,
                text = text,
            )
      }
    }

    return cues
  }

  private fun parseTime(
      h: String,
      m: String,
      s: String,
      ms: String?,
  ): Long {
    var msLong = 0L
    if (!ms.isNullOrEmpty()) {
      msLong =
          when {
            ms.length >= 3 -> ms.take(3).toLong()
            ms.length == 2 -> ms.toLong() * 10
            ms.length == 1 -> ms.toLong() * 100
            else -> 0L
          }
    }
    return (h.toLong() * 3_600_000 + m.toLong() * 60_000 + s.toLong() * 1_000 + msLong)
  }
}
