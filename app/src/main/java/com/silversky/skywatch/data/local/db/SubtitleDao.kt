package com.silversky.skywatch.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SubtitleDao {
  @Query(
      """
        SELECT * FROM media 
        WHERE title = :title 
        AND (year = :year OR (year IS NULL AND :year IS NULL))
        AND (season = :season OR (season IS NULL AND :season IS NULL))
        AND (episode = :episode OR (episode IS NULL AND :episode IS NULL))
        AND (edition = :edition OR (edition IS NULL AND :edition IS NULL))
    """
  )
  suspend fun getMedia(
      title: String,
      year: Int?,
      season: Int?,
      episode: Int?,
      edition: String?,
  ): MediaEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMedia(media: MediaEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubtitle(subtitle: SubtitleEntity)

  @Query("SELECT * FROM subtitles WHERE mediaId = :mediaId")
  suspend fun getSubtitlesForMedia(mediaId: String): List<SubtitleEntity>

  @Query("SELECT * FROM subtitles WHERE id = :id")
  suspend fun getSubtitle(id: String): SubtitleEntity?

  @Query("UPDATE subtitles SET lastUsed = :lastUsed WHERE id = :id")
  suspend fun updateLastUsed(id: String, lastUsed: Long)

  @Query("SELECT * FROM subtitles ORDER BY lastUsed ASC")
  suspend fun getAllSubtitlesSortedByUsage(): List<SubtitleEntity>

  @Query("DELETE FROM subtitles WHERE id = :id") suspend fun deleteSubtitle(id: String)

  @Query("SELECT COUNT(*) FROM subtitles WHERE mediaId = :mediaId")
  suspend fun getSubtitleCountForMedia(mediaId: String): Int

  @Query("DELETE FROM media WHERE id = :id") suspend fun deleteMedia(id: String)

  @Transaction
  suspend fun deleteSubtitleAndMediaIfEmpty(id: String) {
    val subtitle = getSubtitle(id) ?: return
    deleteSubtitle(id)
    if (getSubtitleCountForMedia(subtitle.mediaId) == 0) {
      deleteMedia(subtitle.mediaId)
    }
  }
}
