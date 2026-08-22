package com.silversky.subtitle.server.model

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val port: Int,
    val apiKey: String,
    val mdns: MdnsConfig,
)

@Serializable
data class MdnsConfig(
    val service: String,
    val name: String,
)
