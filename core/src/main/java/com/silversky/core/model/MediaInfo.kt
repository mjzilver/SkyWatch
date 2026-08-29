package com.silversky.core.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface MediaInfo {
  val title: String
  val year: Int?
  val edition: String?
  val entryPath: String

  val season: Int?
    get() = null

  val episode: Int?
    get() = null

  val episodeName: String?
    get() = null
}

@Serializable
data class MovieInfo(
    override val title: String,
    override val year: Int?,
    override val edition: String?,
    override val entryPath: String,
) : MediaInfo

@Serializable
data class EpisodeInfo(
    override val title: String,
    override val year: Int?,
    override val edition: String?,
    override val entryPath: String,
    override val season: Int,
    override val episode: Int,
    override val episodeName: String? = null,
) : MediaInfo
