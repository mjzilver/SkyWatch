package com.silversky.subtitle.server.model

import kotlin.time.Instant

data class CachedSubtitle(
    val id: String,
    val name: String,
    val filePath: String,
    val lastUsed: Instant,
)
