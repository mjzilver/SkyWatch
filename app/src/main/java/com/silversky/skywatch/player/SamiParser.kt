package com.silversky.skywatch.player

import com.silversky.core.logger.Logger

object SamiParser : SubtitleFormatParser {

  override fun canParse(content: String): Boolean =
      content.contains("<SAMI", ignoreCase = true) || content.contains("<SYNC", ignoreCase = true)

  override fun parse(
      content: String,
      logger: Logger?,
  ): List<SubtitleCue> {
    if (content.isBlank()) {
      logger?.debug("SAMI content is empty or blank")
      return emptyList()
    }

    val cues = mutableListOf<SubtitleCue>()
    val normalized = content.replace(Regex("\r\n|\r|\n"), " ")

    var syncIndex = normalized.indexOf("<SYNC", ignoreCase = true)
    var previousStart: Long? = null
    var previousText: String? = null

    while (syncIndex != -1) {
      val startAttribute = normalized.indexOf("Start", syncIndex, ignoreCase = true)
      if (startAttribute == -1) {
        syncIndex = normalized.indexOf("<SYNC", syncIndex + 5, ignoreCase = true)
        continue
      }

      val equalsIndex = normalized.indexOf('=', startAttribute)
      if (equalsIndex == -1) {
        syncIndex = normalized.indexOf("<SYNC", startAttribute + 5, ignoreCase = true)
        continue
      }

      var digitStart = equalsIndex + 1
      while (digitStart < normalized.length && !normalized[digitStart].isDigit()) {
        digitStart++
      }

      if (digitStart >= normalized.length) break

      var digitEnd = digitStart
      while (digitEnd < normalized.length && normalized[digitEnd].isDigit()) {
        digitEnd++
      }

      val startTime = normalized.substring(digitStart, digitEnd).toLongOrNull()
      val tagEnd = normalized.indexOf('>', digitEnd)

      if (tagEnd == -1 || startTime == null) {
        syncIndex = normalized.indexOf("<SYNC", digitEnd, ignoreCase = true)
        continue
      }

      val nextSyncIndex = normalized.indexOf("<SYNC", tagEnd + 1, ignoreCase = true)
      val bodyEnd = if (nextSyncIndex == -1) normalized.length else nextSyncIndex

      val rawText = normalized.substring(tagEnd + 1, bodyEnd).trim()
      val text = cleanSamiText(rawText)

      if (previousStart != null && previousText != null) {
        if (startTime > previousStart) {
          cues.add(SubtitleCue(previousStart, startTime, previousText))
        }
      }

      if (text.isBlank() || text.equals("&nbsp;", ignoreCase = true)) {
        previousStart = null
        previousText = null
      } else {
        previousStart = startTime
        previousText = text
      }

      syncIndex = nextSyncIndex
    }

    if (previousStart != null && previousText != null) {
      cues.add(SubtitleCue(previousStart, previousStart + 2000, previousText))
    }

    logger?.debug("Parsed ${cues.size} SAMI cues")
    return cues
  }

  private fun cleanSamiText(raw: String): String {
    var text = raw
    val pIndex = text.indexOf("<P", ignoreCase = true)
    if (pIndex != -1) {
      val pEnd = text.indexOf('>', pIndex)
      if (pEnd != -1) {
        text = text.substring(pEnd + 1)
      }
    }
    return text.trim()
  }
}
