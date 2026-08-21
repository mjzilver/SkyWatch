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
    val enabled: Boolean = true,
    val service: String = "_skywatch-subtitle._tcp.local.",
    val name: String = "SkyWatch Subtitle Server",
)
