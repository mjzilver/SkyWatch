package com.silversky.core.smb

data class SmbEntry(
    val name: String,
    val isDirectory: Boolean,
    val children: List<SmbEntry> = emptyList(),
    val path: String
)