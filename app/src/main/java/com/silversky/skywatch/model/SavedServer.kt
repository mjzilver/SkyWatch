package com.silversky.skywatch.model

import com.silversky.core.model.SmbServer
import kotlinx.serialization.Serializable

@Serializable
data class SavedServer(
    val server: SmbServer,
    val username: String = "",
    val password: String = "",
    val isGuest: Boolean = false,
)
