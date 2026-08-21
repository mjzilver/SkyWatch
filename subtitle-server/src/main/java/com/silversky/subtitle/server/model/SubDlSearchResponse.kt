package com.silversky.subtitle.server.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubDlSearchResponse(
    val status: Boolean,
    val results: List<SubDlSearchResult>,
    val match: SubDlMatch?,
    val subtitles: List<SubDlSubtitle>,
)

@Serializable
data class SubDlSearchResult(
    @SerialName("sd_id") val sdId: Long,
    val type: String,
    val name: String,
    val year: Int,
    @SerialName("imdb_id") val imdbId: String,
)

@Serializable
data class SubDlMatch(
    val engine: String,
    val confidence: String,
    val degraded: Boolean,
    val type: String,
    val title: String,
    val year: Int,
    val season: Int?,
    val episode: Int?,
    @SerialName("full_season") val fullSeason: Boolean,
    @SerialName("sd_id") val sdId: Long,
    val link: String,
)

@Serializable
data class SubDlSubtitle(
    @SerialName("release_name") val releaseName: String,
    val lang: String,
    @SerialName("match_score") val matchScore: Double,
    val url: String,
)
