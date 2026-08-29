package com.silversky.skywatch.data.repository

import android.content.Context
import com.silversky.core.logger.Logger
import com.silversky.core.model.EpisodeInfo
import com.silversky.core.model.MovieInfo
import com.silversky.core.parser.FilenameParser
import com.silversky.skywatch.config.ConfigLoader
import com.silversky.skywatch.data.local.db.MediaEntity
import com.silversky.skywatch.data.local.db.SubtitleDao
import com.silversky.skywatch.data.local.db.SubtitleEntity
import com.silversky.skywatch.data.remote.SubDlSearchResponse
import com.silversky.skywatch.di.ApplicationScope
import com.silversky.skywatch.model.SubtitleResult
import com.silversky.skywatch.model.SubtitleSearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class SubtitleRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    private val subtitleDao: SubtitleDao,
    private val filenameParser: FilenameParser,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
  private val config by lazy { ConfigLoader.load(context, logger) }
  private val subtitleDir = File(context.filesDir, "subtitles").apply { mkdirs() }

  private val client =
      HttpClient(CIO) {
        install(ContentNegotiation) {
          json(
              Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
              }
          )
        }
        install(Logging) {
          level = LogLevel.INFO
        }
      }

  suspend fun search(query: String): SubtitleSearchResult? =
      withContext(Dispatchers.IO) {
        val currentConfig = config
        if (currentConfig == null || currentConfig.apiKey.isBlank()) {
          logger.error("Subtitle API key missing. Subtitle features disabled.")
          return@withContext null
        }

        val mediaInfo = filenameParser.parse(query).firstOrNull() ?: return@withContext null

        val season: Int?
        val episode: Int?
        when (mediaInfo) {
          is EpisodeInfo -> {
            season = mediaInfo.season
            episode = mediaInfo.episode
          }
          is MovieInfo -> {
            season = null
            episode = null
          }
        }

        val cachedMedia =
            subtitleDao.getMedia(
                mediaInfo.title,
                mediaInfo.year,
                season,
                episode,
                mediaInfo.edition,
            )

        if (cachedMedia != null) {
          val cachedSubtitles = subtitleDao.getSubtitlesForMedia(cachedMedia.id)
          if (cachedSubtitles.isNotEmpty()) {
            logger.debug("Cache hit for $query: ${cachedSubtitles.size} subtitles")
            return@withContext SubtitleSearchResult(
                title = cachedMedia.title,
                year = cachedMedia.year,
                season = cachedMedia.season,
                episode = cachedMedia.episode,
                edition = cachedMedia.edition,
                subtitles = cachedSubtitles.map { SubtitleResult(it.id, it.name) },
            )
          }
        }

        logger.debug("Cache miss for $query, searching SubDl")
        try {
          val response: SubDlSearchResponse =
              client
                  .get("https://api.subdl.com/api/v2/files/search") {
                    parameter("filename", query)
                    parameter("languages", "en")
                    bearerAuth(currentConfig.apiKey)
                  }
                  .body()

          if (!response.status || response.subtitles.isEmpty()) {
            logger.debug("No subtitles found for $query")
            return@withContext null
          }

          val topMatches = response.subtitles.sortedByDescending { it.matchScore }.take(5)

          val mediaId =
              cachedMedia?.id
                  ?: UUID.randomUUID().toString().also { id ->
                    val season: Int?
                    val episode: Int?
                    when (mediaInfo) {
                      is EpisodeInfo -> {
                        season = mediaInfo.season
                        episode = mediaInfo.episode
                      }
                      is MovieInfo -> {
                        season = null
                        episode = null
                      }
                    }

                    subtitleDao.insertMedia(
                        MediaEntity(
                            id = id,
                            title = mediaInfo.title,
                            year = mediaInfo.year,
                            season = season,
                            episode = episode,
                            edition = mediaInfo.edition,
                        )
                    )
                  }

          for (match in topMatches) {
            try {
              val zipResponse = client.get("https://api.subdl.com/${match.url}")
              if (zipResponse.status.value == 200) {
                val zipBytes = zipResponse.body<ByteArray>()
                extractAndSaveSubtitles(zipBytes, mediaId)
              }
            } catch (e: Exception) {
              logger.error("Failed to download match ${match.releaseName}", e)
            }
          }

          val finalSubtitles = subtitleDao.getSubtitlesForMedia(mediaId)

          val season: Int?
          val episode: Int?
          when (mediaInfo) {
            is EpisodeInfo -> {
              season = mediaInfo.season
              episode = mediaInfo.episode
            }
            is MovieInfo -> {
              season = null
              episode = null
            }
          }

          return@withContext SubtitleSearchResult(
              title = mediaInfo.title,
              year = mediaInfo.year,
              season = season,
              episode = episode,
              edition = mediaInfo.edition,
              subtitles = finalSubtitles.map { SubtitleResult(it.id, it.name) },
          )
        } catch (e: Exception) {
          logger.error("Failed to search subtitles for $query", e)
          return@withContext null
        }
      }

  private suspend fun extractAndSaveSubtitles(zipBytes: ByteArray, mediaId: String) {
    ZipInputStream(zipBytes.inputStream()).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (!entry.isDirectory && (entry.name.endsWith(".srt") || entry.name.endsWith(".vtt"))) {
          val output = ByteArrayOutputStream()
          zip.copyTo(output)
          val bytes = output.toByteArray()

          val subtitleId = UUID.randomUUID().toString()
          val file = File(subtitleDir, subtitleId)
          file.writeBytes(bytes)

          subtitleDao.insertSubtitle(
              SubtitleEntity(
                  id = subtitleId,
                  mediaId = mediaId,
                  name = entry.name,
                  filePath = file.absolutePath,
                  lastUsed = System.currentTimeMillis(),
              )
          )
        }
        zip.closeEntry()
        entry = zip.nextEntry
      }
    }
    triggerCleanup()
  }

  suspend fun downloadSubtitle(id: String): ByteArray =
      withContext(Dispatchers.IO) {
        val subtitle = subtitleDao.getSubtitle(id) ?: throw Exception("Subtitle not found")
        val file = File(subtitle.filePath)
        if (!file.exists()) {
          subtitleDao.deleteSubtitleAndMediaIfEmpty(id)
          throw Exception("Subtitle file missing on disk")
        }

        subtitleDao.updateLastUsed(id, System.currentTimeMillis())
        return@withContext file.readBytes()
      }

  private fun triggerCleanup() {
    applicationScope.launch(Dispatchers.IO) {
      try {
        cleanupCache()
      } catch (e: Exception) {
        logger.error("Cache cleanup failed", e)
      }
    }
  }

  private suspend fun cleanupCache() {
    val maxSizeBytes = 512L * 1024 * 1024 // 500MB
    val targetSizeBytes = 256L * 1024 * 1024 // 250MB

    val currentFiles = subtitleDir.listFiles() ?: return
    var totalSize = currentFiles.sumOf { it.length() }

    if (totalSize <= maxSizeBytes) return

    logger.info(
        "Subtitle cache size exceeded (${formatBytes(totalSize)}), cleaning up to ${formatBytes(targetSizeBytes)}"
    )

    val allSubtitles = subtitleDao.getAllSubtitlesSortedByUsage()
    for (sub in allSubtitles) {
      if (totalSize <= targetSizeBytes) break

      val file = File(sub.filePath)
      val size = if (file.exists()) file.length() else 0L

      subtitleDao.deleteSubtitleAndMediaIfEmpty(sub.id)
      if (file.exists()) file.delete()

      totalSize -= size
    }
  }

  private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()

    for (unit in units) {
      value /= 1024

      if (value < 1024) {
        return "%.1f %s".format(value, unit)
      }
    }

    return "%.1f PB".format(value / 1024)
  }
}
