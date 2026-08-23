package com.silversky.subtitle.server.model

data class CachedMedia(
    val id: String,
    val title: String,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    val edition: String?,
    val subtitles: List<CachedSubtitle>,
) {
  override fun toString(): String =
      "CachedMedia(" +
          "id='$id', " +
          "title='$title', " +
          "year=$year, " +
          "season=$season, " +
          "episode=$episode, " +
          "edition=$edition, " +
          "subtitles=${subtitles.size}" +
          ")"
}

fun CachedMedia.toSearchResult(): SubtitleSearchResult =
    SubtitleSearchResult(
        title = title,
        year = year,
        season = season,
        episode = episode,
        edition = edition,
        subtitles =
            subtitles.map {
              SubtitleResult(
                  id = it.id,
                  name = it.name,
              )
            },
    )
