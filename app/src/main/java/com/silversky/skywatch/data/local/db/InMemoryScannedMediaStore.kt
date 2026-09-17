package com.silversky.skywatch.data.local.db

class InMemoryScannedMediaStore : ScannedMediaStore {

  private val data = mutableMapOf<String, List<ScannedMediaEntity>>()

  override suspend fun getMediaForShare(
      serverIp: String,
      shareName: String,
  ): List<ScannedMediaEntity> = data["$serverIp/$shareName"].orEmpty()

  override suspend fun insertAll(
      media: List<ScannedMediaEntity>,
  ) {
    media
        .groupBy { "${it.serverIp}/${it.shareName}" }
        .forEach { (key, entities) ->
          data[key] = entities
        }
  }

  override suspend fun deleteForShare(
      serverIp: String,
      shareName: String,
  ) {
    data.remove("$serverIp/$shareName")
  }
}
