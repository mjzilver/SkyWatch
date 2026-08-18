package com.silversky.skywatch.model

import com.silversky.core.smb.SmbServer

data class SavedServer(
    val server: SmbServer,
    val username: String = "",
    val password: String = ""
)