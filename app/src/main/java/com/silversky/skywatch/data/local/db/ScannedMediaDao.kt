package com.silversky.skywatch.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScannedMediaDao : ScannedMediaStore {
  @Query("SELECT * FROM scanned_media WHERE serverIp = :serverIp AND shareName = :shareName")
  override suspend fun getMediaForShare(
      serverIp: String,
      shareName: String,
  ): List<ScannedMediaEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun insertAll(media: List<ScannedMediaEntity>)

  @Query("DELETE FROM scanned_media WHERE serverIp = :serverIp AND shareName = :shareName")
  override suspend fun deleteForShare(serverIp: String, shareName: String)
}
