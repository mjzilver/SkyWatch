package com.silversky.core.model

data class SmbEntry(
    val name: String,
    val type: SmbEntryType,
    val path: String,
    val size: Long = 0L,
    val dateModified: Long = 0L,
    val isHidden: Boolean = false,
    var shareName: String? = null,
    var ancestors: List<SmbEntry>? = null,
    val children: List<SmbEntry> = emptyList(),
)

enum class SmbEntryType {
  Share,
  Directory,
  File,
}
