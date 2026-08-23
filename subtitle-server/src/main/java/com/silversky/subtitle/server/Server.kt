package com.silversky.subtitle.server

import com.silversky.subtitle.server.config.ConfigLoader
import com.silversky.subtitle.server.parser.FilenameParser
import com.silversky.subtitle.server.parser.TokenClassifier
import com.silversky.subtitle.server.repository.SubtitleRepository
import com.silversky.subtitle.server.routes.SubtitleRoutes
import com.silversky.subtitle.server.service.CacheCleaner
import com.silversky.subtitle.server.service.MdnsService
import com.silversky.subtitle.server.service.SubtitleService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  val config = ConfigLoader.load()
  val repository = SubtitleRepository()
  val filenameParser = FilenameParser(TokenClassifier())
  val cacheCleaner = CacheCleaner(repository, applicationScope)

  val subtitleService =
      SubtitleService(
          config = config,
          repository = repository,
          filenameParser = filenameParser,
      )

  println("Starting Subtitle Server on port ${config.port}...")

  val mdns =
      MdnsService(
          port = config.port,
          config = config.mdns,
      )

  mdns.start()
  cacheCleaner.start()

  embeddedServer(
          factory = Netty,
          host = "0.0.0.0",
          port = config.port,
      ) {
        install(ContentNegotiation) {
          json()
        }

        SubtitleRoutes.configure(
            application = this,
            subtitleService = subtitleService,
        )
      }
      .start(wait = true)
}
