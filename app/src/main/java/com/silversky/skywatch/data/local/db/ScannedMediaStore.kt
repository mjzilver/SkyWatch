package com.silversky.skywatch.data.local.db

interface ScannedMediaStore {
  suspend fun getMediaForShare(
      serverIp: String,
      shareName: String,
  ): List<ScannedMediaEntity>

  suspend fun insertAll(media: List<ScannedMediaEntity>)

  suspend fun deleteForShare(
      serverIp: String,
      shareName: String,
  )
}
