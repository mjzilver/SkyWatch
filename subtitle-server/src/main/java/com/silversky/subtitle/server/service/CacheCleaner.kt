package com.silversky.subtitle.server.service

import com.silversky.subtitle.server.repository.SubtitleRepository
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CacheCleaner(
    private val repository: SubtitleRepository,
    private val scope: CoroutineScope,
) {

  companion object {
    private const val MAX_CACHE_SIZE = 1L * 1024 * 1024 * 1024 // 1 GB
    private const val TARGET_CACHE_SIZE = 750L * 1024 * 1024 // 750 MB
  }

  fun start() {
    scope.launch(Dispatchers.IO) {
      while (isActive) {
        try {
          cleanupSubtitleCache()
        } catch (e: Exception) {
          println("Failed to clean subtitle cache ${e.message}")
        }
        delay(24.hours)
      }
    }
  }

  fun cleanupSubtitleCache() {
    val subtitles = repository.getAllSubtitles()

    var totalSize = subtitles.sumOf {
      val file = File(it.filePath)
      if (file.exists()) {
        file.length()
      } else {
        println("File ${it.filePath} in db does not exist on disk.")
        0L
      }
    }

    println("Cache size: ${formatBytes(totalSize)} / ${formatBytes(MAX_CACHE_SIZE)}")

    if (totalSize <= MAX_CACHE_SIZE) {
      return
    }

    println("Cache exceeded max size, deleting old subtitles...")

    for (subtitle in subtitles) {
      if (totalSize <= TARGET_CACHE_SIZE) {
        break
      }

      val file = File(subtitle.filePath)
      val size = if (file.exists()) file.length() else 0L

      repository.deleteSubtitle(subtitle.id)
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
