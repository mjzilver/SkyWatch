package com.silversky.core.smb

data class SmbServer(
    val name: String?,
    val ipAddress: String,
    val port: Int = 445
)