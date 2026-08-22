package com.silversky.skywatch.model

import kotlinx.serialization.Serializable

@Serializable data class Settings(val subtitleServerAddress: String? = null)
