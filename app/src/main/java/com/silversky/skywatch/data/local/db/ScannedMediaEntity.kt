package com.silversky.skywatch.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_media")
data class ScannedMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverIp: String,
    val shareName: String,
    val entryPath: String,
    val title: String,
    val year: Int?,
    val edition: String?,
    val season: Int?,
    val episode: Int?,
    val episodeName: String?,
    val isMovie: Boolean,
)
