package com.silversky.core.smb

import com.silversky.core.model.MediaInfo
import com.silversky.core.model.SmbEntryType
import com.silversky.core.parser.FilenameParser

class MediaScanner(
    private val client: SmbClient,
    private val parser: FilenameParser,
) {
  fun scan(shareName: String): List<MediaInfo> {
    val result = mutableListOf<MediaInfo>()
    scanRecursive(shareName, "", result)
    return result
  }

  private fun scanRecursive(
      shareName: String,
      path: String,
      result: MutableList<MediaInfo>,
  ) {
    try {
      val entries = client.list(shareName, path)
      for (entry in entries) {
        if (entry.type == SmbEntryType.Directory) {
          scanRecursive(shareName, entry.path, result)
        } else if (entry.type == SmbEntryType.File && isVideoFile(entry.name)) {
          result.addAll(parser.parse(entry.name, entry.path))
        }
      }
    } catch (e: Exception) {
      // Log error or skip directory
    }
  }

  private fun isVideoFile(name: String): Boolean {
    val extensions = listOf("mkv", "mp4", "avi", "mov", "wmv", "m4v")
    return extensions.any { name.lowercase().endsWith(".$it") }
  }
}
