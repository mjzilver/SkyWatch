package com.silversky.skywatch.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubDlSearchResponse(
    val status: Boolean,
    val results: List<SubDlSearchResult> = emptyList(),
    val match: SubDlMatch? = null,
    val subtitles: List<SubDlSubtitle> = emptyList(),
)

@Serializable
data class SubDlSearchResult(
    @SerialName("sd_id") val sdId: Long,
    val type: String,
    val name: String,
    val year: Int,
    @SerialName("imdb_id") val imdbId: String? = null,
)

@Serializable
data class SubDlMatch(
    val engine: String,
    val confidence: String,
    val degraded: Boolean,
    val type: String,
    val title: String,
    val year: Int,
    val season: Int? = null,
    val episode: Int? = null,
    @SerialName("full_season") val fullSeason: Boolean = false,
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
