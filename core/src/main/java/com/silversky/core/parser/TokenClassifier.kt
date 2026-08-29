package com.silversky.core.parser

class TokenClassifier {

  fun classify(value: String): Token =
      when {
        value.matches(Regex("""(?i)S\d{1,2}(?:E\d{1,3})+""")) -> parseSeasonEpisode(value)
        value.matches(Regex("""\(?\d{4}\)?""")) -> Token.Number(value.trim('(', ')').toInt())
        else -> Token.Text(value)
      }

  private fun parseSeasonEpisode(value: String): Token.SeasonEpisode {
    val upperValue = value.uppercase()
    val season = upperValue.substringAfter('S').substringBefore('E').toInt()
    val episodes =
        upperValue.substringAfter('E').split('E').filter { it.isNotEmpty() }.map { it.toInt() }

    return Token.SeasonEpisode(
        season = season,
        episodes = episodes,
    )
  }
}
