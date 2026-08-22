package com.silversky.subtitle.server.routes

import com.silversky.subtitle.server.model.SubtitleSearchResult
import com.silversky.subtitle.server.model.toSearchResult
import com.silversky.subtitle.server.service.SubtitleService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

object SubtitleRoutes {

  fun configure(
      application: Application,
      subtitleService: SubtitleService,
  ) {
    application.routing {
      get("/api/search") {
        val query = call.request.queryParameters["query"]

        if (query == null) {
          call.respondText(
              "Missing query parameter",
              status = HttpStatusCode.BadRequest,
          )
          return@get
        }

        val result = subtitleService.search(query)

        if (result == null) {
          call.respond(
              SubtitleSearchResult(
                  title = query,
                  year = null,
                  season = null,
                  episode = null,
                  subtitles = emptyList(),
              )
          )
          return@get
        }

        println("Found $result")

        call.respond(result.toSearchResult())
      }

      get("/api/request/{id}") {
        val id = call.parameters["id"]

        if (id == null) {
          call.respondText(
              "Missing subtitle id",
              status = HttpStatusCode.BadRequest,
          )
          return@get
        }

        val subtitle = subtitleService.getSubtitle(id)

        if (subtitle == null) {
          call.respondText(
              "Subtitle not found",
              status = HttpStatusCode.NotFound,
          )
          return@get
        }

        call.respondBytes(
            bytes = subtitle.file,
            contentType = ContentType.Text.Plain,
        )
      }
    }
  }
}
