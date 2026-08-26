package com.silversky.core.parser

sealed interface Token {
  data class Text(
      val value: String,
  ) : Token

  data class Number(
      val value: Int,
  ) : Token

  data class SeasonEpisode(
      val season: Int,
      val episode: Int,
  ) : Token
}
