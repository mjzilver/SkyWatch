package com.silversky.skywatch.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    indices = [Index(value = ["title", "year", "season", "episode", "edition"], unique = true)],
)
data class MediaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    val edition: String?,
)

@Entity(
    tableName = "subtitles",
    foreignKeys =
        [
            ForeignKey(
                entity = MediaEntity::class,
                parentColumns = ["id"],
                childColumns = ["mediaId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("mediaId")],
)
data class SubtitleEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val name: String,
    val filePath: String,
    val lastUsed: Long,
)
