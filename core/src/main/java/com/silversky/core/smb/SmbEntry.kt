package com.silversky.core.smb

data class SmbEntry(
    val name: String,
    val isDirectory: Boolean,
    val path: String,
    val size: Long = 0L,
    val dateModified: Long = 0L,
    val isHidden: Boolean = false,
)
