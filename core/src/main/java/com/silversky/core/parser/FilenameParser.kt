package com.silversky.core.parser

import com.silversky.core.model.EpisodeInfo
import com.silversky.core.model.MediaInfo
import com.silversky.core.model.MovieInfo

class FilenameParser {

  fun parse(filename: String, path: String = ""): List<MediaInfo> {
    if (isNoiseFile(filename, path)) return emptyList()

    val name = filename.substringBeforeLast('.', filename)
    val match = findSeasonEpisodeMatch(name)

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
    val episodeSequence = groups.last()
    val episodes = extractEpisodes(episodeSequence)

    val candidate = selectBestTitle(titlePart, path, filename) ?: return emptyList()
    val (episodeName, edition) = parseMetadata(restPart)

    return listOf(
        EpisodeInfo(
            title = candidate.title,
            year = candidate.year,
            season = season,
            episodes = episodes,
            episodeName = episodeName,
            edition = edition,
            entryPath = path,
        )
    )
  }

  private fun parseMovie(
      name: String,
      path: String,
      filename: String,
  ): List<MediaInfo> {
    val candidate = selectBestTitle(name, path, filename) ?: return emptyList()
    val (_, edition) = parseMetadata(name)

    return listOf(
        MovieInfo(
            title = candidate.title,
            year = candidate.year,
            edition = edition,
            entryPath = path,
        )
    )
  }

  private fun selectBestTitle(
      namePart: String,
      path: String,
      filename: String,
  ): TitleCandidate? {
    val intersection = extractIntersectionCandidate(namePart, path)
    val filenameTitle = createTitleCandidate(namePart, sourceWeight = 1.0)
    val pathTitle = extractTitleFromPath(path, filename)

    return listOfNotNull(intersection, filenameTitle, pathTitle)
        .maxByOrNull { it.confidence }
        ?.takeIf { it.confidence >= MIN_CONFIDENCE }
  }

  private fun extractIntersectionCandidate(
      filenameTitle: String,
      path: String,
  ): TitleCandidate? {
    if (path.isBlank()) return null

    val (cleanFilenameTitle, year) = parseTitleAndYear(filenameTitle)
    val filenameTokens =
        tokenize(cleanFilenameTitle).map { it.lowercase() }.filter { it.length > 1 }
    if (filenameTokens.isEmpty()) return null

    val segments = path.replace('\\', '/').split('/').filter { it.isNotBlank() }
    var bestIntersection: List<String>? = null
    var bestMatchCount = 0

    for (segment in segments) {
      val (cleanPathTitle, _) = parseTitleAndYear(segment)
      val pathTokens = tokenize(cleanPathTitle).map { it.lowercase() }
      val intersection = filenameTokens.filter { it in pathTokens }

      if (intersection.size > bestMatchCount) {
        bestMatchCount = intersection.size
        bestIntersection = pathTokens.filter { it in intersection }
      }
    }

    if (bestIntersection.isNullOrEmpty()) return null

    val resultTitle =
        bestIntersection.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    return TitleCandidate(
        title = resultTitle,
        year = year,
        confidence = 0.9 + (bestMatchCount * 0.02).coerceAtMost(0.1),
    )
  }

  private fun extractTitleFromPath(path: String, filename: String): TitleCandidate? {
    if (path.isBlank()) return null

    val segments = path.replace('\\', '/').split('/').filter { it.isNotBlank() }
    val folderSegments =
        if (segments.lastOrNull()?.equals(filename, ignoreCase = true) == true) {
          segments.dropLast(1)
        } else {
          segments
        }

    return folderSegments
        .asReversed()
        .asSequence()
        .mapIndexedNotNull { index, segment ->
          val candidate = createTitleCandidate(segment, sourceWeight = PATH_WEIGHT)
          candidate?.copy(
              confidence = (candidate.confidence - index * PATH_DEPTH_PENALTY).coerceAtLeast(0.0)
          )
        }
        .firstOrNull()
  }

  private fun createTitleCandidate(input: String, sourceWeight: Double): TitleCandidate? {
    val (title, year) = parseTitleAndYear(input)
    if (!isSensibleTitle(title)) return null

    return TitleCandidate(
        title = title,
        year = year,
        confidence = calculateTitleConfidence(title, year) * sourceWeight,
    )
  }

  private fun parseTitleAndYear(input: String): Pair<String, Int?> {
    val cleanedInput = stripWebsitePrefix(input)
    val tokens = tokenize(cleanedInput)
    val yearIndex = findYearIndex(tokens)
    val stopIndex = findTitleStopIndex(tokens, yearIndex)

    val skipUntil = calculateWebsiteSkip(tokens, stopIndex)
    val titleTokens = tokens.subList(skipUntil, stopIndex)

    val finalTitleTokens = titleTokens.ifEmpty {
      if (yearIndex > 0) tokens.take(yearIndex) else tokens.take(stopIndex).ifEmpty { tokens }
    }

    val year = if (yearIndex != -1) tokens[yearIndex].toIntOrNull() else null
    return finalTitleTokens.joinToString(" ") to year
  }

  private fun calculateTitleConfidence(title: String, year: Int?): Double {
    if (!isSensibleTitle(title)) return 0.0
    val letters = title.count { it.isLetter() }

    var confidence =
        when {
          letters < 2 -> 0.0
          letters < 4 -> 0.35
          letters < 6 -> 0.55
          letters < 10 -> 0.70
          else -> 0.75
        }

    val words = title.split(WHITESPACE).filter { it.isNotBlank() }
    if (words.size >= 2) confidence += 0.15
    if (words.size >= 3) confidence += 0.05
    if (year != null) confidence += 0.10

    return confidence.coerceAtMost(1.0)
  }

  private fun parseMetadata(input: String): Pair<String?, String?> {
    val tokens = tokenize(input)
    var edition: String? = null
    val extraTokens = mutableListOf<String>()
    var metadataStarted = false

    for (token in tokens) {
      val lower = token.lowercase()
      if (edition == null) {
        edition = editionKeywords.firstOrNull { lower.contains(it) }
      }

      if (lower in strongMarkers) {
        metadataStarted = true
        continue
      }
      if (!metadataStarted) extraTokens.add(token)
    }

    return extraTokens.joinToString(" ").ifBlank { null } to edition
  }

  private fun isSensibleTitle(title: String): Boolean {
    val lower = title.trim().lowercase()
    if (lower.isBlank() || lower.length < 2) return false
    if (lower in noiseFolders) return false
    if (lower.matches(SHORT_SEASON_EP_PATTERN)) return false
    if (lower.matches(SEASON_FOLDER_PATTERN)) return false
    if (lower.all { it.isDigit() }) return false
    return true
  }

  private fun isNoiseFile(filename: String, path: String): Boolean {
    val base = filename.substringBeforeLast('.', filename).trim().lowercase()
    if (base in noiseFilenames) return true
    if (path.isBlank()) return false
    return path
        .replace('\\', '/')
        .split('/')
        .filter { it.isNotBlank() }
        .any { it.trim().lowercase() in noiseFolders }
  }

  private fun stripWebsitePrefix(input: String): String {
    if (!input.contains(" - ")) return input
    val split = input.split(" - ")
    if (split.size < 2) return input
    val prefix = split[0].trim().lowercase()
    return if (prefix.startsWith("www.") || TLD_PATTERN.find(prefix) != null) {
      input.substringAfter(" - ").trim()
    } else input
  }

  private fun calculateWebsiteSkip(tokens: List<String>, stopIndex: Int): Int {
    if (tokens.size <= 1) return 0
    var skip = 0
    for (i in 0 until minOf(stopIndex, 3)) {
      val lower = tokens[i].lowercase()
      if (lower == "www" || lower in tlds) skip = i + 1
    }
    return skip
  }

  private fun tokenize(input: String): List<String> =
      input
          .replace('-', ' ')
          .split(TOKEN_DELIMITERS)
          .filter { it.isNotBlank() }
          .map { it.trim('(', ')', '[', ']', '{', '}') }

  private fun findYearIndex(tokens: List<String>): Int = tokens.indexOfLast {
    it.length == 4 && it.all { c -> c.isDigit() } && it.toInt() in 1900..2099
  }

  private fun findTitleStopIndex(tokens: List<String>, yearIndex: Int): Int {
    val noiseIndex = tokens.indexOfFirst {
      it.lowercase() in strongMarkers ||
          it.matches(SEASON_PATTERN) ||
          it.matches(SHORT_SEASON_EP_PATTERN)
    }
    return when {
      yearIndex != -1 && noiseIndex != -1 -> minOf(yearIndex, noiseIndex)
      yearIndex != -1 -> yearIndex
      noiseIndex != -1 -> noiseIndex
      else -> tokens.size
    }
  }

  private fun extractEpisodes(sequence: String): List<Int> =
      sequence
          .split(EPISODE_SPLIT_PATTERN)
          .mapNotNull { it.toIntOrNull() }
          .filter { it != 0 }
          .distinct()

  private fun findSeasonEpisodeMatch(name: String): MatchResult? =
      seasonEpisodeRegex.find(name)
          ?: seasonXEpisodeRegex.find(name)
          ?: explicitSeasonEpisodeRegex.find(name)

  private data class TitleCandidate(val title: String, val year: Int?, val confidence: Double)

  private companion object {
    const val MIN_CONFIDENCE = 0.45
    const val PATH_WEIGHT = 1.5
    const val PATH_DEPTH_PENALTY = 0.10

    val WHITESPACE = Regex("""\s+""")
    val TOKEN_DELIMITERS = Regex("""[.\s_]""")
    val EPISODE_SPLIT_PATTERN = Regex("""[e&-]""", RegexOption.IGNORE_CASE)
    val SEASON_PATTERN = Regex("""^S\d{1,3}$|^Season$|^Season\d{1,3}$""", RegexOption.IGNORE_CASE)
    val TLD_PATTERN = Regex("""\.(com|org|net|tv|me|io|info|biz)$""", RegexOption.IGNORE_CASE)
    val SHORT_SEASON_EP_PATTERN = Regex("""^S\d{1,3}(?:E\d{1,3})?$""", RegexOption.IGNORE_CASE)
    val SEASON_FOLDER_PATTERN = Regex("""Season\s*\d+""", RegexOption.IGNORE_CASE)

    const val EPISODE_REGEX = """\d{1,3}"""
    const val EPISODE_SEQUENCE_REGEX = """$EPISODE_REGEX(?:(?:E|[&-])E?$EPISODE_REGEX)*"""

    val seasonEpisodeRegex =
        Regex(
            """S(\d{1,2})([. X])?E($EPISODE_SEQUENCE_REGEX)(?=[. &-]|$)""",
            RegexOption.IGNORE_CASE,
        )
    val seasonXEpisodeRegex =
        Regex("""\b(\d{1,2})X($EPISODE_SEQUENCE_REGEX)(?=[. &-]|$)""", RegexOption.IGNORE_CASE)
    val explicitSeasonEpisodeRegex =
        Regex(
            """SEASON\s*(\d{1,2})(?:\s*EPISODE)?\s*($EPISODE_SEQUENCE_REGEX)""",
            RegexOption.IGNORE_CASE,
        )

    val editionKeywords =
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

    val strongMarkers =
        setOf(
            "2160p",
            "1080p",
            "720p",
            "576p",
            "480p",
            "360p",
            "4k",
            "uhd",
            "x264",
            "x265",
            "h264",
            "h265",
            "hevc",
            "avc",
            "av1",
            "vp9",
            "mpeg2",
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
            "bluray",
            "bdrip",
            "brip",
            "webrip",
            "webdl",
            "web-dl",
            "hdtv",
            "dvdrip",
            "remux",
            "xvid",
            "divx",
            "hdr",
            "hdr10",
            "hdr10+",
            "dolbyvision",
            "10bit",
            "8bit",
            "hi10p",
        )

    val noiseFolders =
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

    val noiseFilenames = setOf("sample", "trailer")
    val tlds = setOf("com", "org", "net", "tv", "me", "io", "info", "biz")
  }
}
