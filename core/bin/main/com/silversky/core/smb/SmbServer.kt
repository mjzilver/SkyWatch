package com.silversky.core.smb

data class SmbServer(
    var name: String?,
    val ipAddress: String,
    val port: Int = 445
)