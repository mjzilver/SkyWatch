package com.silversky.cli

import com.silversky.core.logger.Logger
import com.silversky.core.model.EpisodeInfo
import com.silversky.core.model.MovieInfo
import com.silversky.core.model.SmbEntry
import com.silversky.core.model.SmbEntryType
import com.silversky.core.model.SmbFile
import com.silversky.core.model.SmbServer
import com.silversky.core.parser.FilenameParser
import com.silversky.core.parser.TokenClassifier
import com.silversky.core.smb.MediaScanner
import com.silversky.core.smb.SmbClient
import com.silversky.core.smb.SmbScanner
import com.silversky.core.utils.NetworkUtils.Companion.resolveHostName
import java.io.FileOutputStream

class Cli(private val logger: Logger) {
  private var servers = emptyList<SmbServer>()
  private var client: SmbClient? = null
  private var share: String? = null
  private var currentPath = ""
  private var openedFile: SmbFile? = null

  private val classifier = TokenClassifier()
  private val parser = FilenameParser(classifier)

  fun run() {
    println("SkyWatch CLI")
    println("Type help for available commands.")

    while (true) {
      print("> ")

      val input = readlnOrNull()?.trim() ?: break

      if (input.isEmpty()) {
        continue
      }

      val parts = input.split(" ", limit = 2)
      val command = parts[0].lowercase()
      val arguments = parts.getOrNull(1)

      when (command) {
        "scan" -> scan()
        "scan-media" -> scanMedia()
        "info" -> info()
        "cd" -> cd(arguments)
        "connect" -> connect(arguments)
        "ls",
        "list" -> list(arguments)
        "shares" -> listShares()
        "tree" -> tree()
        "use" -> useShare(arguments)
        "open" -> open(arguments)
        "download" -> download(arguments)
        "read" -> read(arguments)
        "disconnect" -> disconnect()
        "help" -> help()
        "exit",
        "quit" -> break
        else -> println("Unknown command: $command")
      }
    }

    client?.close()
  }

  private fun scan() {
    println("Scanning...")

    val scanner = SmbScanner()
    servers = scanner.scanNetwork(logger)

    if (servers.isEmpty()) {
      println("No SMB servers found.")
      return
    }

    servers.forEachIndexed { index, server ->
      println("${index + 1}. ${server.name ?: "Unknown"} (${server.ipAddress}:${server.port})")
    }
  }

  private fun connect(arguments: String?) {
    if (arguments == null) {
      println("Usage: connect <server|ip> <username> <password>")
      return
    }

    val parts = arguments.split(" ")

    if (parts.size < 3) {
      println("Usage: connect <server|ip> <username> <password>")
      return
    }

    val serverArg = parts[0]

    val server =
        serverArg.toIntOrNull()?.let { index ->
          servers.getOrNull(index - 1)
        }
            ?: SmbServer(
                ipAddress = serverArg,
                name = resolveHostName(logger, serverArg),
            )

    val username = parts[1]
    val password = parts[2]

    client?.close()
    client = SmbClient(logger)

    try {
      client!!.connect(server, username, password)

      listShares()

      print("Share: ")
      share = readlnOrNull()?.trim()

      currentPath = ""

      println("Connected to ${server.name ?: server.ipAddress}.")
    } catch (e: Exception) {
      println("Connection failed: ${e.message}")
      client = null
    }
  }

  private fun cd(path: String?) {
    val client =
        client
            ?: run {
              println("Not connected.")
              return
            }

    val share =
        share
            ?: run {
              println("No share selected.")
              return
            }

    if (path == null) {
      println("Usage: cd <path>")
      return
    }

    val newPath =
        when {
          path == ".." -> {
            currentPath.trimEnd('/').substringBeforeLast('/', "")
          }

          path.startsWith("/") -> {
            path.trim('/')
          }

          currentPath.isEmpty() -> {
            path.trim('/')
          }

          else -> {
            "${currentPath.trimEnd('/')}/${path.trim('/')}"
          }
        }

    try {
      val entries = client.list(share, newPath)

      currentPath = newPath

      entries.forEach {
        val prefix = if (it.type == SmbEntryType.Directory) "DIR" else "   "
        println("$prefix ${it.name}")
      }
    } catch (e: Exception) {
      println("Failed to enter directory: ${e.message}")
    }
  }

  private fun tree() {
    val client =
        client
            ?: run {
              println("Not connected.")
              return
            }

    val share =
        share
            ?: run {
              println("No share selected.")
              return
            }

    try {
      println("Building tree for $share...")
      val tree = client.listTree(share)

      tree.forEach { entry ->
        printEntry(entry, "")
      }
    } catch (e: Exception) {
      println("Failed to build tree: ${e.message}")
    }
  }

  private fun printEntry(entry: SmbEntry, prefix: String) {
    val label =
        when (entry.type) {
          SmbEntryType.Directory -> "${entry.name}/"
          SmbEntryType.File -> entry.name
          SmbEntryType.Share -> "smb://${entry.name}"
        }

    println("$prefix└── $label")

    entry.children.forEach { child ->
      printEntry(child, prefix + "    ")
    }
  }

  private fun scanMedia() {
    val client =
        client
            ?: run {
              println("Not connected.")
              return
            }

    val share =
        share
            ?: run {
              println("No share selected.")
              return
            }

    println("Scanning media in $share...")

    val scanner = MediaScanner(client, parser)
    val media = scanner.scan(share)

    if (media.isEmpty()) {
      println("No media found.")
      return
    }

    media
        .groupBy { it.title }
        .forEach { (title, items) ->
          val first = items.first()
          val year = first.year?.let { " ($it)" } ?: ""
          val edition = first.edition?.let { " [$it]" } ?: ""

          if (items.size == 1 && first is MovieInfo) {
            println("MOVIE: $title$year$edition")
          } else {
            println("SERIES: $title$year")
            items
                .filterIsInstance<EpisodeInfo>()
                .sortedWith(compareBy({ it.season }, { it.episode }))
                .forEach { ep ->
                  println(
                      "  S${ep.season.toString().padStart(2, '0')}E${ep.episode.toString().padStart(2, '0')} - ${ep.entryPath}"
                  )
                }
          }
        }
  }

  private fun list(path: String?) {
    val client =
        client
            ?: run {
              println("Not connected.")
              return
            }

    val share =
        share
            ?: run {
              println("No share selected.")
              return
            }

    if (path != null) {
      currentPath = path
    }

    try {
      val entries = client.list(share, currentPath)

      entries.forEach {
        val prefix = if (it.type == SmbEntryType.Directory) "DIR" else "   "
        println("$prefix ${it.name}")
      }
    } catch (e: Exception) {
      println("Failed to list directory: ${e.message}")
    }
  }

  private fun info() {
    val client =
        client
            ?: run {
              println("Not connected.")
              return
            }

    println(
        "Server: ${client.server?.name ?: "Unknown"} (${client.server?.ipAddress}:${client.server?.port})"
    )
    println("Share:  ${share ?: "None"}")
    println("Path:   ${currentPath.ifEmpty { "/" }}")
  }

  private fun listShares() {
    val client =
        client
            ?: run {
              println("Not connected.")
              return
            }

    println("Available shares on ${client.server?.ipAddress}")
    client.listShares().forEach { s -> println(s.name) }
  }

  private fun useShare(share: String?) {
    this.share = share
    currentPath = ""
  }

  private fun open(path: String?) {
    if (path == null) {
      println("Usage: open <path>")
      return
    }

    val client =
        client
            ?: run {
              println("Not connected.")
              return
            }

    val share =
        share
            ?: run {
              println("No share selected.")
              return
            }

    val fullPath =
        if (path.startsWith("/")) {
          path.trim('/')
        } else if (currentPath.isEmpty()) {
          path
        } else {
          "${currentPath.trimEnd('/')}/${path.trimStart('/')}"
        }

    try {
      openedFile?.close()
      openedFile = client.openFile(share, fullPath)

      println("File: $fullPath")
      println("Size: ${openedFile!!.size} bytes")
    } catch (e: Exception) {
      openedFile = null
      println("Failed to open file: ${e.message}")
    }
  }

  private fun read(arguments: String?) {
    val file =
        openedFile
            ?: run {
              println("No file opened.")
              return
            }

    if (arguments == null) {
      println("Usage: read <offset> <length>")
      return
    }

    val parts = arguments.split(" ")

    if (parts.size != 2) {
      println("Usage: read <offset> <length>")
      return
    }

    val offset = parts[0].toLongOrNull()
    val length = parts[1].toIntOrNull()

    if (offset == null || length == null || offset < 0 || length <= 0) {
      println("Invalid offset or length.")
      return
    }

    if (offset >= file.size) {
      println("Offset is beyond end of file.")
      return
    }

    val actualLength =
        minOf(
            length,
            (file.size - offset).toInt(),
        )

    val buffer = ByteArray(actualLength)

    try {
      val bytesRead =
          file.read(
              filePosition = offset,
              buffer = buffer,
              bufferOffset = 0,
              length = actualLength,
          )

      println("Read $bytesRead bytes:")

      println(buffer.take(bytesRead).joinToString(" ") { "%02x".format(it) })
    } catch (e: Exception) {
      println("Failed to read file: ${e.message}")
    }
  }

  private fun download(arguments: String?) {
    val file =
        openedFile
            ?: run {
              println("No file opened.")
              return
            }

    if (arguments == null) {
      println("Usage: download <local-path>")
      return
    }

    val buffer = ByteArray(1024 * 1024)

    try {
      FileOutputStream(arguments).use { output ->
        var offset = 0L
        var totalRead = 0L

        while (offset < file.size) {
          val length = minOf(buffer.size.toLong(), file.size - offset).toInt()

          val bytesRead =
              file.read(
                  filePosition = offset,
                  buffer = buffer,
                  bufferOffset = 0,
                  length = length,
              )

          if (bytesRead <= 0) {
            throw RuntimeException("Unexpected end of file at offset $offset")
          }

          output.write(buffer, 0, bytesRead)

          offset += bytesRead
          totalRead += bytesRead

          val percent = (totalRead * 100 / file.size).toInt()
          print("\rDownloading: $percent% ($totalRead / ${file.size} bytes)")
        }

        println()
        println("Downloaded to $arguments")
      }
    } catch (e: Exception) {
      println()
      println("Download failed: ${e.message}")
    }
  }

  private fun disconnect() {
    client?.close()
    client = null
    share = null
    currentPath = ""
    openedFile?.close()
    openedFile = null

    println("Disconnected.")
  }

  private fun help() {
    println(
        """
        Commands:
          scan                                Scan network for SMB servers
          scan-media                          Scan current share for movies and series
          connect <server> <user> <password>  Connect to an SMB server
          list [path]                         List files
          shares                              List shares
          use <share>                         Use share folder
          open <path>                         Open a file
          read <offset> <length>              Read bytes from opened file
          info                                Show current connection
          disconnect                          Disconnect
          help                                Show this help
          exit                                Exit
        """
            .trimIndent()
    )
  }
}
