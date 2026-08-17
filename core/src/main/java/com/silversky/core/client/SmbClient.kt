package com.silversky.core.smb

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.connection.Connection
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbServer

class SmbClient(
    private val server: SmbServer,
    private val logger: Logger
) : AutoCloseable {

    private val client = SMBClient()
    private var connection: Connection? = null

    fun connect() {
        if (connection != null) {
            logger.warn("Tried to connect to connected server ${server.name} (${server.ipAddress})")
            return
        }

        logger.info("Connecting to ${server.name} (${server.ipAddress})")

        connection = client.connect(server.ipAddress)

        logger.info("Connected to ${server.name}")
    }

    fun disconnect() {
        connection?.close()
        connection = null

        logger.debug("Disconnected from ${server.name}")
    }

    override fun close() {
        disconnect()
        client.close()
    }
}