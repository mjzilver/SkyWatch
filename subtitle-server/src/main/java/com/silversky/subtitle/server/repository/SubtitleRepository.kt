package com.silversky.subtitle.server.repository

import com.silversky.subtitle.server.model.CachedMedia
import com.silversky.subtitle.server.model.CachedSubtitle
import com.silversky.subtitle.server.model.MediaInfo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.io.File
import java.util.UUID
import kotlin.time.Clock

class SubtitleRepository(
    databasePath: String = "cache/subtitles.db",
    private val cacheDirectory: String = "cache/subtitles",
) {
  private object Media : Table("media") {
    val id = varchar("id", 36)
    val title = varchar("title", 255)
    val year = integer("year").nullable()
    val season = integer("season").nullable()
    val episode = integer("episode").nullable()

    override val primaryKey = PrimaryKey(id)
  }

  private object Subtitles : Table("subtitles") {
    val id = varchar("id", 36)
    val mediaId = varchar("media_id", 36)
    val name = varchar("name", 1000)
    val filePath = varchar("file_path", 1000)
    val lastUsed = long("last_used")

    override val primaryKey = PrimaryKey(id)
  }

  private val database: Database

  init {
    val file = File(databasePath)

    file.parentFile?.mkdirs()
    File(cacheDirectory).mkdirs()

    database =
        Database.connect(
            url = "jdbc:sqlite:${file.path}",
            driver = "org.sqlite.JDBC",
        )

    transaction(database) {
      SchemaUtils.create(
          Media,
          Subtitles,
      )
    }
  }

  fun get(media: MediaInfo): CachedMedia? =
      transaction(database) {
        val mediaRow =
            Media.selectAll()
                .where {
                  (Media.title eq media.title) and
                      (Media.year eq media.year) and
                      (Media.season eq media.season) and
                      (Media.episode eq media.episode)
                }
                .singleOrNull() ?: return@transaction null

        val mediaId = mediaRow[Media.id]

        val subtitles =
            Subtitles.selectAll()
                .where {
                  Subtitles.mediaId eq mediaId
                }
                .map {
                  CachedSubtitle(
                      id = it[Subtitles.id],
                      name = it[Subtitles.name],
                      filePath = it[Subtitles.filePath],
                      lastUsed = kotlin.time.Instant.fromEpochMilliseconds(it[Subtitles.lastUsed]),
                  )
                }

        CachedMedia(
            id = mediaId,
            title = mediaRow[Media.title],
            year = mediaRow[Media.year],
            season = mediaRow[Media.season],
            episode = mediaRow[Media.episode],
            subtitles = subtitles,
        )
      }

  fun saveMedia(media: MediaInfo) {
    transaction(database) {
      val existingMedia =
          Media.selectAll()
              .where {
                (Media.title eq media.title) and
                    (Media.year eq media.year) and
                    (Media.season eq media.season) and
                    (Media.episode eq media.episode)
              }
              .singleOrNull()

      if (existingMedia == null) {
        Media.insert {
          it[id] = UUID.randomUUID().toString()
          it[title] = media.title
          it[year] = media.year
          it[season] = media.season
          it[episode] = media.episode
        }
      }
    }
  }

  fun save(
      media: MediaInfo,
      name: String,
      file: ByteArray,
  ): CachedSubtitle {
    val now = Clock.System.now()
    val subtitleId = UUID.randomUUID().toString()

    val path =
        File(
            cacheDirectory,
            subtitleId,
        )

    path.writeBytes(file)

    transaction(database) {
      val existingMedia =
          Media.selectAll()
              .where {
                (Media.title eq media.title) and
                    (Media.year eq media.year) and
                    (Media.season eq media.season) and
                    (Media.episode eq media.episode)
              }
              .singleOrNull()

      val mediaId =
          existingMedia?.get(Media.id)
              ?: UUID.randomUUID().toString().also { id ->
                Media.insert {
                  it[Media.id] = id
                  it[title] = media.title
                  it[year] = media.year
                  it[season] = media.season
                  it[episode] = media.episode
                }
              }

      Subtitles.insert {
        it[id] = subtitleId
        it[Subtitles.mediaId] = mediaId
        it[Subtitles.name] = name
        it[filePath] = path.path
        it[lastUsed] = now.toEpochMilliseconds()
      }
    }

    return CachedSubtitle(
        id = subtitleId,
        name = name,
        filePath = path.path,
        lastUsed = now,
    )
  }

  fun getSubtitle(id: String): CachedSubtitle? =
      transaction(database) {
        val row =
            Subtitles.selectAll()
                .where {
                  Subtitles.id eq id
                }
                .singleOrNull() ?: return@transaction null

        val now = Clock.System.now()

        Subtitles.update(
            where = {
              Subtitles.id eq id
            }
        ) {
          it[lastUsed] = now.toEpochMilliseconds()
        }

        CachedSubtitle(
            id = row[Subtitles.id],
            name = row[Subtitles.name],
            filePath = row[Subtitles.filePath],
            lastUsed = now,
        )
      }

  fun deleteSubtitle(id: String) {
    transaction(database) {
      val row =
          Subtitles.selectAll()
              .where {
                Subtitles.id eq id
              }
              .singleOrNull() ?: return@transaction

      File(row[Subtitles.filePath]).delete()

      Subtitles.deleteWhere {
        Subtitles.id eq id
      }
    }
  }
}
