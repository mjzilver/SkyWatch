package com.silversky.subtitle.server.parser

class TokenClassifier {

  fun classify(value: String): Token =
      when {
        value.matches(Regex("""(?i)S\d{1,2}E\d{1,3}""")) -> parseSeasonEpisode(value)
        value.matches(Regex("""\(?\d{4}\)?""")) -> Token.Number(value.trim('(', ')').toInt())
        else -> Token.Text(value)
      }

  private fun parseSeasonEpisode(value: String): Token.SeasonEpisode {
    val season = value.substringAfter('S').substringBefore('E').toInt()
    val episode = value.substringAfter('E').toInt()

    return Token.SeasonEpisode(
        season = season,
        episode = episode,
    )
  }
}
