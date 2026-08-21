package com.silversky.subtitle.server.model

data class CachedMedia(
    val id: String,
    val title: String,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    val subtitles: List<CachedSubtitle>,
)

fun CachedMedia.toSearchResult(): SubtitleSearchResult =
    SubtitleSearchResult(
        title = title,
        year = year,
        season = season,
        episode = episode,
        subtitles =
            subtitles.map {
              SubtitleResult(
                  id = it.id,
                  name = it.name,
              )
            },
    )
