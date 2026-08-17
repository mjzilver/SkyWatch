package com.silversky.cli

import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbServer
import com.silversky.core.utils.NetworkUtils.Companion.resolveHostName

class Cli(
    private val logger: Logger
) {
    private var servers = emptyList<SmbServer>()
    private var client: SmbClient? = null
    private var share: String? = null
    private var currentPath = ""

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
                "info" -> info()
                "connect" -> connect(arguments)
                "list" -> list(arguments)
                "shares" -> listShares()
                "use" -> useShare(arguments)
                "disconnect" -> disconnect()
                "help" -> help()
                "exit", "quit" -> break
                else -> println("Unknown command: $command")
            }
        }

        client?.close()
    }

    private fun scan() {
        println("Scanning...")

        val scanner = SmbScanner()
        servers = scanner.scanNetwork()

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
            println("Usage: /connect <server|ip> <username> <password>")
            return
        }

        val parts = arguments.split(" ")

        if (parts.size < 3) {
            println("Usage: /connect <server|ip> <username> <password>")
            return
        }

        val serverArg = parts[0]

        val server = serverArg.toIntOrNull()?.let { index ->
            servers.getOrNull(index - 1)
        } ?: SmbServer(
            ipAddress = serverArg, name = resolveHostName(serverArg)
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

    private fun list(path: String?) {
        val client = client ?: run {
            println("Not connected.")
            return
        }

        val share = share ?: run {
            println("No share selected.")
            return
        }

        if (path != null) {
            currentPath = path
        }

        try {
            val entries = client.list(share, currentPath)

            entries.forEach {
                val prefix = if (it.isDirectory) "DIR" else "   "
                println("$prefix ${it.name}")
            }
        } catch (e: Exception) {
            println("Failed to list directory: ${e.message}")
        }
    }

    private fun info() {
        val client = client ?: run {
            println("Not connected.")
            return
        }

        println("Server: ${client.server?.name ?: "Unknown"} (${client.server?.ipAddress}:${client.server?.port})")
        println("Share:  ${share ?: "None"}")
        println("Path:   ${currentPath.ifEmpty { "/" }}")
    }

    private fun listShares() {
        val client = client ?: run {
            println("Not connected.")
            return
        }

        println("Available shares on ${client.server?.ipAddress}")
        client.listShares().forEach { s -> println(s) }
    }

    private fun useShare(share: String?) {
        this.share = share
        currentPath = ""
    }

    private fun disconnect() {
        client?.close()
        client = null
        share = null
        currentPath = ""

        println("Disconnected.")
    }

    private fun help() {
        println(
            """
            Commands:
              scan                                Scan network for SMB servers
              connect <server> <user> <password>  Connect to an SMB server
              list [path]                         List files
              shares                              List shares
              use <share>                         Use share folder
              disconnect                          Disconnect
              help                                Show this help
              exit                                Exit
            """.trimIndent()
        )
    }
}