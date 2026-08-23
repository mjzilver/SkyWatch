package com.silversky.skywatch.model

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleSearchResult(
    val title: String,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    val edition: String?,
    val subtitles: List<SubtitleResult>,
)

@Serializable
data class SubtitleResult(
    val id: String,
    val name: String,
)
