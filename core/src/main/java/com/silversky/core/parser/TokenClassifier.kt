package com.silversky.core.parser

class TokenClassifier {

  fun classify(value: String): Token {
    if (value.matches(Regex("""(?i)S\d{1,2}(?:E\d{1,3})+"""))) {
      return parseSeasonEpisode(value)
    }

    val cleaned = value.trim('(', ')', '[', ']', '{', '}')
    if (cleaned.length == 4 && cleaned.all { it.isDigit() }) {
      val num = cleaned.toInt()
      if (num in 1900..2099) {
        return Token.Number(num)
      }
    }

    return Token.Text(value)
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
