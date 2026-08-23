package com.silversky.skywatch.player

import com.silversky.core.logger.Logger

data class SubtitleCue(
    val startTime: Long,
    val endTime: Long,
    val text: String,
)

interface SubtitleFormatParser {
  fun canParse(content: String): Boolean

  fun parse(content: String, logger: Logger?): List<SubtitleCue>
}
