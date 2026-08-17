package com.silversky.cli

import com.silversky.cli.logger.ConsoleLogger
import com.silversky.core.client.SmbClient
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbEntry

fun main() {
    val logger = ConsoleLogger()
    val smbScanner = SmbScanner()
    val servers = smbScanner.scanNetwork()

    println("Found ${servers.size} SMB servers:")
    servers.forEachIndexed { index, server ->
        println("${index + 1}. ${server.name ?: "Unknown"} (${server.ipAddress}:${server.port})")
    }

    print("Select a server to connect to (1-${servers.size}): ")
    val selectedIndex = readlnOrNull()?.toIntOrNull()?.minus(1) ?: -1
    if (selectedIndex !in servers.indices) {
        println("Invalid selection. Exiting.")
        return
    }

    val selectedServer = servers[selectedIndex]
    val smbClient = SmbClient(selectedServer, logger)

    print("Enter username (or leave blank for guest): ")
    val username = readlnOrNull()
    val password = if (username != null) {
        print("Enter password: ")
        readlnOrNull()
    } else null
    print("Enter share folder: ")
    val shareName = readlnOrNull()

    if (username != null && password != null && shareName != null) {
        smbClient.connect(username, password, shareName)

        printTree(smbClient.enumerate(shareName))

        smbClient.disconnect()
    } else {
        println("Guest access is not implemented in this example. Exiting.")
    }
    
    println("Exiting.")
}

fun printTree(entries: List<SmbEntry>) {
    println(".")

    fun printEntries(entries: List<SmbEntry>, prefix: String) {
        for (entry in entries) {
            val marker = if (entry.isDirectory) "***" else "---"

            println("$prefix$marker ${entry.name}")

            if (entry.children.isNotEmpty()) {
                printEntries(
                    entry.children,
                    "$prefix---"
                )
            }
        }
    }

    printEntries(entries, "")
}