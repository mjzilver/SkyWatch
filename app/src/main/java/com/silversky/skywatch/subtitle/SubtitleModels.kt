package com.silversky.skywatch.subtitle

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleSearchResult(
    val title: String,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    val subtitles: List<SubtitleResult>,
)

@Serializable
data class SubtitleResult(
    val id: String,
    val name: String,
)
