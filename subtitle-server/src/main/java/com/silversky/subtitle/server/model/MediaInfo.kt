package com.silversky.subtitle.server.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaInfo(
    val title: String,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    val edition: String?,
)
