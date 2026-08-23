package com.silversky.subtitle.server.parser

import com.silversky.subtitle.server.model.MediaInfo

class FilenameParser(
    private val classifier: TokenClassifier,
) {

  private val editionKeywords =
      listOf(
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

  fun parse(filename: String): MediaInfo {
    val tokens =
        filename.split(".", " ", "_", "-").filter { it.isNotBlank() }.map(classifier::classify)
    val seasonEpisode = tokens.filterIsInstance<Token.SeasonEpisode>().firstOrNull()
    val seasonEpisodeIndex = tokens.indexOfFirst { it is Token.SeasonEpisode }.takeIf { it >= 0 }

    val yearIndex =
        tokens
            .mapIndexed { index, token -> index to token }
            .lastOrNull { (_, token) ->
              token is Token.Number && token.value in 1900..2099
            }
            ?.first

    var titleEndIndex =
        listOfNotNull(
                yearIndex,
                seasonEpisodeIndex,
            )
            .minOrNull() ?: tokens.size

    if (titleEndIndex == 0 && yearIndex == 0) {
      titleEndIndex = 1
    }

    val titleTokens =
        tokens.take(titleEndIndex).filter {
          it is Token.Text || it is Token.Number
        }

    val title =
        titleTokens.joinToString(" ") {
          when (it) {
            is Token.Text -> it.value
            is Token.Number -> it.value.toString()
            else -> error("Unexpected token in title: $it")
          }
        }

    val year = yearIndex?.let {
      if (it < titleEndIndex) null else (tokens[it] as Token.Number).value
    }

    val edition =
        tokens
            .filterIsInstance<Token.Text>()
            .map { it.value.lowercase() }
            .firstOrNull { text -> editionKeywords.any { keyword -> text.contains(keyword) } }
            ?.let { found ->
              editionKeywords.first { found.contains(it) }
            }

    return MediaInfo(
        title = title,
        year = year,
        season = seasonEpisode?.season,
        episode = seasonEpisode?.episode,
        edition = edition,
    )
  }
}
