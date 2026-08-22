package com.silversky.skywatch.data.repository

import com.silversky.core.logger.Logger
import com.silversky.skywatch.model.SubtitleSearchResult
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Singleton
class SubtitleRepository
@Inject
constructor(
    private val logger: Logger,
    private val settingsRepository: SettingsRepository,
    private val subtitleServerDiscovery: com.silversky.skywatch.data.remote.SubtitleServerDiscovery,
) {
  private val _autoDiscoveredAddress = MutableStateFlow<String?>(null)
  val autoDiscoveredAddress: StateFlow<String?> = _autoDiscoveredAddress.asStateFlow()

  private val _manualAddress = MutableStateFlow<String?>(null)
  private val _serverAddress = MutableStateFlow<String?>(null)
  private val scope = CoroutineScope(Dispatchers.Main)

  init {
    scope.launch {
      settingsRepository.settings.collect { settings ->
        val newManualAddress = settings.subtitleServerAddress?.takeIf { it.isNotBlank() }
        _manualAddress.value = newManualAddress

        if (newManualAddress != null) {
          stopDiscovery()
          _serverAddress.value = newManualAddress
        } else {
          _serverAddress.value = _autoDiscoveredAddress.value
          startDiscovery()
        }
      }
    }

    scope.launch {
      _autoDiscoveredAddress.collect { autoAddress ->
        if (_manualAddress.value == null) {
          _serverAddress.value = autoAddress
        }
      }
    }
  }

  private val _isDiscovering = MutableStateFlow(false)
  val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

  fun startDiscovery() {
    if (_isDiscovering.value) return
    _isDiscovering.value = true
    logger.debug("Starting background subtitle server discovery")
    subtitleServerDiscovery.start { ip, port ->
      val address = "$ip:$port"
      scope.launch {
        if (healthCheck(address)) {
          logger.info("Auto-discovered valid subtitle server: $address")
          _autoDiscoveredAddress.value = address
        }
      }
    }
  }

  fun stopDiscovery() {
    if (!_isDiscovering.value) return
    _isDiscovering.value = false
    logger.debug("Stopping background subtitle server discovery")
    subtitleServerDiscovery.stop()
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
