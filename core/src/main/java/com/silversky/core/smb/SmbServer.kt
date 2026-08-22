package com.silversky.core.smb

import kotlinx.serialization.Serializable

@Serializable
data class SmbServer(
    var name: String?,
    val ipAddress: String,
    val port: Int = 445,
)
