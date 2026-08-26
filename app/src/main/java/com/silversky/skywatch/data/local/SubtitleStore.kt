package com.silversky.skywatch.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CachedSubtitle(
    val id: String,
    val name: String,
    val content: String,
)

@Singleton
class SubtitleStore @Inject constructor(@ApplicationContext private val context: Context) {
  private val baseDir = File(context.cacheDir, "subtitles")

  init {
    if (!baseDir.exists()) {
      baseDir.mkdirs()
    }
  }

  private fun getFolder(filename: String): File {
    val folder = File(baseDir, sanitizeFilename(filename))
    if (!folder.exists()) {
      folder.mkdirs()
    }
    return folder
  }

  suspend fun saveSubtitle(filename: String, subtitleName: String, content: String) =
      withContext(Dispatchers.IO) {
        val folder = getFolder(filename)
        val file = File(folder, "$subtitleName.srt")
        file.writeText(content)
      }

  suspend fun getCachedSubtitles(filename: String): List<CachedSubtitle> =
      withContext(Dispatchers.IO) {
        val folder = getFolder(filename)
        folder
            .listFiles { file -> file.extension == "srt" }
            ?.map { file ->
              CachedSubtitle(
                  id = file.absolutePath,
                  name = file.nameWithoutExtension,
                  content = file.readText(),
              )
            } ?: emptyList()
      }

  private fun sanitizeFilename(filename: String): String {
    return filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
  }
}
