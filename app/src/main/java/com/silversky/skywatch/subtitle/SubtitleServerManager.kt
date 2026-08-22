package com.silversky.skywatch.subtitle

import com.silversky.core.logger.Logger
import com.silversky.skywatch.settings.SettingsManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleServerManager
@Inject
constructor(
    private val logger: Logger,
    private val settingsManager: SettingsManager,
) {
  private val _serverAddress = MutableStateFlow<String?>(null)

  private val scope = CoroutineScope(Dispatchers.Main)

  init {
    scope.launch {
      settingsManager.settings.collect { settings ->
        _serverAddress.value = settings.subtitleServerAddress
      }
    }
  }

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

  private fun formatUrl(address: String, path: String): String {
    val base =
        if (!address.startsWith("http://") && !address.startsWith("https://")) {
              "http://$address"
            } else {
              address
            }
            .removeSuffix("/")

    val cleanPath = path.removePrefix("/")
    return "$base/$cleanPath"
  }

  suspend fun search(query: String): SubtitleSearchResult? {
    val address =
        _serverAddress.value
            ?: throw IllegalStateException("No available subtitle server (check settings)")
    try {
      val response: HttpResponse =
          client.get(formatUrl(address, "api/search")) {
            url { parameters.append("query", query) }
          }
      if (response.status.isSuccess()) {
        val result = runCatching {
          response.body<SubtitleSearchResult>()
        }
            .getOrElse {
              runCatching {
                val list = response.body<List<SubtitleSearchResult>>()
                if (list.isNotEmpty()) {
                  val first = list.first()
                  SubtitleSearchResult(
                      title = first.title,
                      year = first.year,
                      season = first.season,
                      episode = first.episode,
                      subtitles = list.flatMap { it.subtitles }.distinctBy { it.id },
                  )
                } else null
              }
                  .getOrNull()
            }

        if (result == null || result.subtitles.isEmpty()) {
          return null
        }
        return result
      } else {
        logger.error("Subtitle search failed with status ${response.status}")
        throw Exception("Server error: ${response.status}")
      }
    } catch (e: Exception) {
      if (e is IllegalStateException) throw e
      logger.error("Failed to search subtitles for $query", e)
      throw Exception("No available subtitle server (check settings)")
    }
  }

  suspend fun downloadSubtitle(id: String): ByteArray {
    val address =
        _serverAddress.value
            ?: throw IllegalStateException("No available subtitle server (check settings)")
    try {
      val response: HttpResponse = client.get(formatUrl(address, "api/request/$id"))
      if (response.status.isSuccess()) {
        return response.body()
      } else {
        logger.error("Subtitle download failed with status ${response.status}")
        throw Exception("Download failed: ${response.status}")
      }
    } catch (e: Exception) {
      if (e is IllegalStateException) throw e
      logger.error("Failed to download subtitle $id", e)
      throw Exception("No available subtitle server (check settings)")
    }
  }

  suspend fun healthCheck(address: String): Boolean {
    try {
      val response: HttpResponse = client.get(formatUrl(address, "api/health"))
      return response.status.isSuccess()
    } catch (e: Exception) {
      logger.error("Error during health check", e)
      return false
    }
  }
}
