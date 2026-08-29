package com.silversky.skywatch.model

import kotlinx.serialization.Serializable

@Serializable
enum class SortBy {
  Name,
  DateModified,
  Size,
}

@Serializable
enum class SortOrder {
  Ascending,
  Descending,
}

@Serializable
enum class MediaPriority {
  MoviesFirst,
  SeriesFirst,
  None,
}

@Serializable
data class Settings(
    val sortBy: SortBy = SortBy.Name,
    val sortOrder: SortOrder = SortOrder.Ascending,
    val foldersFirst: Boolean = false,
    val mediaPriority: MediaPriority = MediaPriority.MoviesFirst,
    val subtitleFontSize: Int = 24,
    val subtitleFontFamily: String = "Sans Serif",
)
