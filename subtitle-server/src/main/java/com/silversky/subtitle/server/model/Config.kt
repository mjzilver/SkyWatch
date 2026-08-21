package com.silversky.subtitle.server.model

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val port: Int,
    val apiKey: String,
)
