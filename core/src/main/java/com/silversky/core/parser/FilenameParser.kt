package com.silversky.core.parser

import com.silversky.core.model.EpisodeInfo
import com.silversky.core.model.MediaInfo
import com.silversky.core.model.MovieInfo

class FilenameParser {

  private val editionKeywords =
      setOf(
          "extended",
          "remastered",
          "remaster",
          "director",
          "theatrical",
          "unrated",
          "uncut",
          "criterion",
          "special",
          "collector",
          "final",
          "ultimate",
          "anniversary",
          "hybrid",
          "imax",
      )

  private val strongMarkers =
      setOf(
          // Video quality
          "2160p",
          "1080p",
          "720p",
          "576p",
          "480p",
          "360p",
          "4k",
          "uhd",

          // Video codecs
          "x264",
          "x265",
          "h264",
          "h265",
          "hevc",
          "avc",
          "av1",
          "vp9",
          "mpeg2",

          // Audio codecs
          "aac",
          "ac3",
          "eac3",
          "dd",
          "ddp",
          "dts",
          "dtshd",
          "truehd",
          "flac",
          "opus",
          "mp3",

          // Sources
          "bluray",
          "bdrip",
          "brip",
          "webrip",
          "webdl",
          "web-dl",
          "hdtv",
          "dvdrip",
          "remux",

          // Release tags
          "xvid",
          "divx",

          // Dynamic range
          "hdr",
          "hdr10",
          "hdr10+",
          "dolbyvision",

          // Encoding
          "10bit",
          "8bit",
          "hi10p",
      )

  private val seasonEpisodeRegex =
      Regex(
          """S(\d{1,2})([. X])?E(\d{1,3})(?:E(\d{1,3}))?(?:E(\d{1,3}))?(?=[. ]|$)""",
          RegexOption.IGNORE_CASE,
      )

  private val seasonXEpisodeRegex =
      Regex(
          """\b(\d{1,2})X(\d{1,3})(?:-(\d{1,3}))?(?=[. ]|$)""",
          RegexOption.IGNORE_CASE,
      )

  private val explicitSeasonEpisodeRegex =
      Regex(
          """SEASON\s*(\d{1,2})\s*EPISODE\s*(\d{1,3})""",
          RegexOption.IGNORE_CASE,
      )

  fun parse(filename: String, path: String = ""): List<MediaInfo> {
    val name = filename.substringBeforeLast('.', filename)

    val match =
        seasonEpisodeRegex.find(name)
            ?: seasonXEpisodeRegex.find(name)
            ?: explicitSeasonEpisodeRegex.find(name)

    return if (match != null) {
      val titlePart = name.substring(0, match.range.first).trim()
      val restPart = name.substring(match.range.last + 1).trim()

      val groups = match.groupValues.drop(1)
      val season = groups.first().toInt()
      val episodes = groups.drop(1).mapNotNull { it.toIntOrNull() }.filter { it != 0 }

      val (title, year) = parseTitleAndYear(titlePart)
      val (episodeName, edition) = parseMetadata(restPart)

      episodes.map { episode ->
        EpisodeInfo(
            title = title,
            year = year,
            season = season,
            episode = episode,
            episodeName = episodeName,
            edition = edition,
            entryPath = path,
        )
      }
    } else {
      val (title, year) = parseTitleAndYear(name)
      val (_, edition) = parseMetadata(name)
      listOf(
          MovieInfo(
              title = title,
              year = year,
              edition = edition,
              entryPath = path,
          )
      )
    }
  }

  private fun parseTitleAndYear(input: String): Pair<String, Int?> {
    val tokens =
        input
            .replace('-', ' ')
            .split(".", " ", "_")
            .filter { it.isNotBlank() }
            .map { it.trim('(', ')', '[', ']', '{', '}') }

    val yearIndex = tokens.indexOfLast {
      it.length == 4 && it.all { c -> c.isDigit() } && it.toInt() in 1900..2099
    }

    val titleTokens = if (yearIndex != -1) tokens.take(yearIndex) else tokens
    val year = if (yearIndex != -1) tokens[yearIndex].toInt() else null

    val finalTitleTokens = titleTokens.ifEmpty {
      if (yearIndex != -1) listOf(tokens[yearIndex]) else tokens
    }
    val title = finalTitleTokens.joinToString(" ")

    return title to year
  }

  private fun parseMetadata(input: String): Pair<String?, String?> {
    val tokens =
        input
            .replace('-', ' ')
            .split(".", " ", "_")
            .filter { it.isNotBlank() }
            .map { it.trim('(', ')', '[', ']', '{', '}') }

    var edition: String? = null
    val extraTokens = mutableListOf<String>()
    var metadataStarted = false

    for (token in tokens) {
      val lower = token.lowercase()

      val foundEdition = editionKeywords.firstOrNull { lower.contains(it) }
      if (foundEdition != null) {
        if (edition == null) edition = foundEdition
      }

      if (lower in strongMarkers) {
        metadataStarted = true
        continue
      }

      if (!metadataStarted) {
        extraTokens.add(token)
      }
    }

    val episodeName = extraTokens.joinToString(" ").ifBlank { null }
    return episodeName to edition
  }
}
