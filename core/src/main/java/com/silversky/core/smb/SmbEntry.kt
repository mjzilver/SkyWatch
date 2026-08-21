package com.silversky.core.smb

data class SmbEntry(
    val name: String,
    val isDirectory: Boolean,
    val path: String,
    val isHidden: Boolean = false,
)
