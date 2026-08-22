package com.silversky.skywatch.settings

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val subtitleServerAddress: String? = null
)
