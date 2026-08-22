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
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

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

    // Nothing cached, search SubDL.
    var response =
        client.get("https://api.subdl.com/api/v2/files/search") {
          parameter("filename", filename)
          parameter("languages", "en")
          bearerAuth(config.apiKey)
        }

    var body = response.bodyAsText()
    var result = json.decodeFromString<SubDlSearchResponse>(body)

    // If filename search yielded nothing, try searching by parsed title.
    if (result.subtitles.isEmpty() && media.title.isNotBlank()) {
      println("Filename search failed, trying title search: ${media.title} (${media.year})")
      response =
          client.get("https://api.subdl.com/api/v2/files/search") {
            parameter(
                "query",
                if (media.year != null) "${media.title} ${media.year}" else media.title,
            )
            parameter("languages", "en")
            if (media.season != null) parameter("season", media.season)
            if (media.episode != null) parameter("episode", media.episode)
            bearerAuth(config.apiKey)
          }
      body = response.bodyAsText()
      result = json.decodeFromString<SubDlSearchResponse>(body)
    }

    println("Received ${result.subtitles.size} subtitles for $filename")

    val matches =
        result.subtitles
            .sortedByDescending { it.matchScore }
            .take(5) // Download top 5 for better variety

    if (matches.isEmpty()) {
      println("No subtitles found for $filename, caching negative result.")
      repository.saveMedia(media)
      return null
    }

    println("Downloading ${matches.size} top matches...")

    for (match in matches) {
      try {
        println("Downloading match: ${match.releaseName} (${match.matchScore})")
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

          println("Found subtitle: ${entry.name}")

          val subtitleMedia = filenameParser.parse(entry.name)

          println("Parsed subtitle: $subtitleMedia")

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
