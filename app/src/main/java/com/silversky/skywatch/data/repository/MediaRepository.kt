package com.silversky.skywatch.data.repository

import com.silversky.core.model.EpisodeInfo
import com.silversky.core.model.MediaInfo
import com.silversky.core.model.MovieInfo
import com.silversky.core.parser.FilenameParser
import com.silversky.core.smb.MediaScanner
import com.silversky.core.smb.SmbClient
import com.silversky.skywatch.data.local.db.ScannedMediaDao
import com.silversky.skywatch.data.local.db.ScannedMediaEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class MediaRepository
@Inject
constructor(
    private val scannedMediaDao: ScannedMediaDao,
    private val filenameParser: FilenameParser,
) {
  suspend fun getMediaForShare(serverIp: String, shareName: String): List<MediaInfo> =
      withContext(Dispatchers.IO) {
        val entities = scannedMediaDao.getMediaForShare(serverIp, shareName)

        entities.map { entity ->
          if (entity.isMovie) {
            MovieInfo(
                title = entity.title,
                year = entity.year,
                edition = entity.edition,
                entryPath = entity.entryPath,
            )
          } else {
            EpisodeInfo(
                title = entity.title,
                year = entity.year,
                edition = entity.edition,
                entryPath = entity.entryPath,
                season = entity.season ?: 1,
                episode = entity.episode ?: 1,
                episodeName = entity.episodeName,
            )
          }
        }
      }

  suspend fun scanAndSave(
      client: SmbClient,
      serverIp: String,
      shareName: String,
  ): List<MediaInfo> =
      withContext(Dispatchers.IO) {
        val scanner = MediaScanner(client, filenameParser)
        val media = scanner.scan(shareName)

        val entities = media.map { info ->
          when (info) {
            is MovieInfo -> {
              ScannedMediaEntity(
                  serverIp = serverIp,
                  shareName = shareName,
                  entryPath = info.entryPath,
                  title = info.title,
                  year = info.year,
                  edition = info.edition,
                  season = null,
                  episode = null,
                  episodeName = null,
                  isMovie = true,
              )
            }
            is EpisodeInfo -> {
              ScannedMediaEntity(
                  serverIp = serverIp,
                  shareName = shareName,
                  entryPath = info.entryPath,
                  title = info.title,
                  year = info.year,
                  edition = info.edition,
                  season = info.season,
                  episode = info.episode,
                  episodeName = info.episodeName,
                  isMovie = false,
              )
            }
          }
        }

        scannedMediaDao.deleteForShare(serverIp, shareName)
        scannedMediaDao.insertAll(entities)

        media
      }
}
