package com.silversky.skywatch.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaDao {
  @Query("SELECT * FROM media_info WHERE serverIp = :serverIp AND shareName = :shareName")
  suspend fun getMediaForShare(serverIp: String, shareName: String): List<MediaEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(media: List<MediaEntity>)

  @Query("DELETE FROM media_info WHERE serverIp = :serverIp AND shareName = :shareName")
  suspend fun deleteForShare(serverIp: String, shareName: String)
}
