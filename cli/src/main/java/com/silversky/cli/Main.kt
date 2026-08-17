package com.silversky.cli

import com.silversky.cli.logger.ConsoleLogger
import com.silversky.core.smb.SmbClient
import com.silversky.core.smb.SmbScanner

fun main() {
    val logger = ConsoleLogger()
    val smbScanner = SmbScanner()

    val servers = smbScanner.scanNetwork()

    for (server in servers) {
        val smbClient = SmbClient(server, logger)

        try {
            smbClient.connect()
            // Perform operations with the connected server
        } catch (e: Exception) {
            logger.error("Failed to connect to ${server.name} (${server.ipAddress}:${server.port}): ${e.message}")
        } finally {
            smbClient.disconnect()
        }
    }
}