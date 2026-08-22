package com.silversky.subtitle.server.config

import com.silversky.subtitle.server.model.Config
import java.io.File
import kotlinx.serialization.json.Json

object ConfigLoader {
  fun load(path: String = "config/config.json"): Config {
    val file = File(path)

    require(file.exists()) {
      "Config file not found: ${file.absolutePath}"
    }

    return Json.decodeFromString<Config>(file.readText())
  }
}
