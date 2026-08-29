package com.silversky.skywatch.player

import com.silversky.core.logger.Logger

object SubtitleParser {
  private val parsers = listOf(SrtParser, SamiParser)

  fun parse(content: String, logger: Logger? = null): List<SubtitleCue> {
    val parser = parsers.find { it.canParse(content) }
    if (parser == null) {
      logger?.error("No suitable parser found for subtitle content")
      return emptyList()
    }
    return parser.parse(content, logger)
  }
}
