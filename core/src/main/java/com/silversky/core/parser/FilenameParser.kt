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

  private val noiseFolders =
      setOf(
          "extra",
          "extras",
          "subs",
          "subtitles",
          "trailers",
          "metadata",
          "backdrops",
          "deleted",
          "featurettes",
          "behind the scenes",
          "screens",
          "shorts",
          "samples",
          "bonus",
          "promo",
          "other",
          "others",
      )

  private val noiseFilenames =
      setOf(
          "sample",
          "trailer",
      )

  private val seasonFolderRegex = Regex("""Season\s*\d+""", RegexOption.IGNORE_CASE)

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
    if (isNoiseFile(filename, path)) {
      return emptyList()
    }

    val name = filename.substringBeforeLast('.', filename)

    val match =
        seasonEpisodeRegex.find(name)
            ?: seasonXEpisodeRegex.find(name)
            ?: explicitSeasonEpisodeRegex.find(name)

    return if (match != null) {
      parseEpisode(name, match, path, filename)
    } else {
      parseMovie(name, path, filename)
    }
  }

  private fun parseEpisode(
      name: String,
      match: MatchResult,
      path: String,
      filename: String,
  ): List<MediaInfo> {
    val titlePart = name.substring(0, match.range.first).trim()
    val restPart = name.substring(match.range.last + 1).trim()

    val groups = match.groupValues.drop(1)
    val season = groups.first().toInt()
    val episodes = groups.drop(1).mapNotNull { it.toIntOrNull() }.filter { it != 0 }

    val (parsedTitle, parsedYear) = parseTitleAndYear(titlePart)
    val (title, year) = resolveTitle(parsedTitle, parsedYear, path, filename)

    if (!isSensibleTitle(title)) {
      return emptyList()
    }

    val (episodeName, edition) = parseMetadata(restPart)

    return episodes.map { episode ->
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
  }

  private fun parseMovie(
      name: String,
      path: String,
      filename: String,
  ): List<MediaInfo> {
    val (parsedTitle, parsedYear) = parseTitleAndYear(name)
    val (title, year) = resolveTitle(parsedTitle, parsedYear, path, filename)

    if (!isSensibleTitle(title)) {
      return emptyList()
    }

    val (_, edition) = parseMetadata(name)

    return listOf(
        MovieInfo(
            title = title,
            year = year,
            edition = edition,
            entryPath = path,
        )
    )
  }

  private fun resolveTitle(
      parsedTitle: String,
      parsedYear: Int?,
      path: String,
      filename: String,
  ): Pair<String, Int?> {
    if (isSensibleTitle(parsedTitle)) {
      return parsedTitle to parsedYear
    }

    return extractTitleFromPath(path, filename) ?: (parsedTitle to parsedYear)
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

    val titleTokens =
        if (yearIndex != -1) {
          tokens.take(yearIndex)
        } else {
          tokens
        }

    val year =
        if (yearIndex != -1) {
          tokens[yearIndex].toInt()
        } else {
          null
        }

    val finalTitleTokens = titleTokens.ifEmpty {
      if (yearIndex != -1) {
        listOf(tokens[yearIndex])
      } else {
        tokens
      }
    }

    return finalTitleTokens.joinToString(" ") to year
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

      if (foundEdition != null && edition == null) {
        edition = foundEdition
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

  private fun isSensibleTitle(title: String): Boolean {
    val normalized = title.trim()

    if (normalized.isBlank()) {
      return false
    }

    if (normalized.length < 2) {
      return false
    }

    if (normalized.lowercase() in noiseFolders) {
      return false
    }

    if (normalized.matches(Regex("""[SE]\d+""", RegexOption.IGNORE_CASE))) {
      return false
    }

    if (normalized.matches(seasonFolderRegex)) {
      return false
    }

    if (normalized.all(Char::isDigit)) {
      return false
    }

    return true
  }

  private fun isNoiseFile(filename: String, path: String): Boolean {
    val baseFilename = filename.substringBeforeLast('.', filename).trim().lowercase()

    if (baseFilename in noiseFilenames) {
      return true
    }

    if (path.isBlank()) {
      return false
    }

    val segments = path.replace('\\', '/').split('/').filter { it.isNotBlank() }

    return segments.any { it.trim().lowercase() in noiseFolders }
  }

  private fun extractTitleFromPath(
      path: String,
      filename: String,
  ): Pair<String, Int?>? {
    if (path.isBlank()) {
      return null
    }

    val segments = path.replace('\\', '/').split('/').filter { it.isNotBlank() }

    val folderSegments =
        if (segments.lastOrNull()?.equals(filename, ignoreCase = true) == true) {
          segments.dropLast(1)
        } else {
          segments
        }

    for (segment in folderSegments.asReversed()) {
      val (title, year) = parseTitleAndYear(segment)

      if (isSensibleTitle(title)) {
        return title to year
      }
    }

    return null
  }
}
