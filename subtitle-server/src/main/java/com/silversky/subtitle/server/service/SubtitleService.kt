package com.silversky.subtitle.server.service

import com.silversky.subtitle.server.model.CachedMedia
import com.silversky.subtitle.server.model.Config
import com.silversky.subtitle.server.model.SubDlSearchResponse
import com.silversky.subtitle.server.model.SubtitleFile
import com.silversky.subtitle.server.parser.FilenameParser
import com.silversky.subtitle.server.repository.SubtitleRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json

class SubtitleService(
    private val config: Config,
    private val repository: SubtitleRepository,
    private val filenameParser: FilenameParser,
) {
  private val client =
      HttpClient(CIO) {
        install(Logging) {
          level = LogLevel.ALL
        }
      }

  private val json = Json {
    ignoreUnknownKeys = true
  }

  suspend fun search(filename: String): CachedMedia? {
    val media = filenameParser.parse(filename)

    println("Parsed media: $media")

    repository.get(media)?.let { cached ->
      println("Cache hit: ${cached.subtitles.size} subtitle(s)")

      return cached
    }

    println("Cache miss: $media")

    val response =
        client.get("https://api.subdl.com/api/v2/files/search") {
          parameter("filename", filename)
          parameter("languages", "en")
          bearerAuth(config.apiKey)
        }

    val result = json.decodeFromString<SubDlSearchResponse>(response.bodyAsText())
    println("Received ${result.subtitles.size} subtitles for $filename")

    val matches = result.subtitles.sortedByDescending { it.matchScore }.take(5)

    if (matches.isEmpty()) {
      println("No subtitles found for $filename, caching negative result.")
      repository.saveMedia(media)
      return null
    }

    for (match in matches) {
      try {
        val fileResponse = client.get("https://api.subdl.com/${match.url}")
        if (fileResponse.status.value == 200) {
          val zipBytes = fileResponse.body<ByteArray>()
          extractSubtitles(zipBytes)
        }
      } catch (e: Exception) {
        println("Failed to download match ${match.releaseName}: ${e.message}")
      }
    }

    return repository.get(media)
  }

  private fun extractSubtitles(
      zipBytes: ByteArray,
  ) {
    ZipInputStream(zipBytes.inputStream()).use { zip ->
      var entry = zip.nextEntry

      while (entry != null) {
        if (!entry.isDirectory) {
          val output = ByteArrayOutputStream()

          zip.copyTo(output)

          val bytes = output.toByteArray()
          val subtitleMedia = filenameParser.parse(entry.name)

          if (subtitleMedia.title.isNotBlank()) {
            repository.save(
                media = subtitleMedia,
                name = entry.name,
                file = bytes,
            )
          }
        }

        zip.closeEntry()
        entry = zip.nextEntry
      }
    }
  }

  fun getSubtitle(id: String): SubtitleFile? {
    val subtitle = repository.getSubtitle(id) ?: return null

    return SubtitleFile(
        name = subtitle.name,
        file = File(subtitle.filePath).readBytes(),
    )
  }
}
