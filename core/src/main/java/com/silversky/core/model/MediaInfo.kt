package com.silversky.core.model

sealed interface MediaInfo {
  val title: String
  val year: Int?
  val edition: String?
  val entryPath: String
}

data class MovieInfo(
    override val title: String,
    override val year: Int?,
    override val edition: String?,
    override val entryPath: String,
) : MediaInfo

data class EpisodeInfo(
    override val title: String,
    override val year: Int?,
    override val edition: String?,
    override val entryPath: String,
    val season: Int,
    val episode: Int,
    val episodeName: String? = null,
) : MediaInfo
