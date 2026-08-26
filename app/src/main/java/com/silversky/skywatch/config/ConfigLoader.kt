package com.silversky.skywatch.config

import android.content.Context
import com.silversky.core.logger.Logger
import java.io.IOException
import kotlinx.serialization.json.Json

object ConfigLoader {
  private val json = Json { ignoreUnknownKeys = true }

  fun load(context: Context, logger: Logger): Config? {
    return try {
      context.assets.open("config.json").use { inputStream ->
        val content = inputStream.bufferedReader().use { it.readText() }
        json.decodeFromString<Config>(content)
      }
    } catch (e: IOException) {
      logger.warn("config.json not found in assets: ${e.message}")
      null
    } catch (e: Exception) {
      logger.error("Failed to load config", e)
      null
    }
  }
}
