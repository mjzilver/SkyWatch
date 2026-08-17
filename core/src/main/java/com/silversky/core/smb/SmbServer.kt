package com.silversky.core.smb

import com.hierynomus.smbj.connection.Connection

data class SmbServer(
    val name: String?,
    val ipAddress: String,
    val port: Int
)