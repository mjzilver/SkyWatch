package com.silversky.core.client

import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbServer

class SmbClient(
    private val logger: Logger
) : AutoCloseable {

    private val client = SMBClient()

    var server: SmbServer? = null
    private var connection: Connection? = null
    private var session: Session? = null

    fun connect(server: SmbServer, username: String, password: String) {
        if (connection != null) {
            logger.warn("Already connected")
            return
        }

        logger.info(
            "Connecting to ${server.name ?: server.ipAddress} " +
                    "(${server.ipAddress}:${server.port})"
        )

        try {
            val conn = client.connect(server.ipAddress, server.port)

            val authenticationContext =
                AuthenticationContext(
                    username,
                    password.toCharArray(),
                    null
                )

            val authenticatedSession =
                conn.authenticate(authenticationContext)

            this.server = server
            this.connection = conn
            this.session = authenticatedSession

            logger.info(
                "Connected to ${server.name ?: server.ipAddress} as $username"
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to connect to " +
                        "${server.name ?: server.ipAddress}: ${e.message}"
            )

            connection?.close()
            connection = null
            session = null
            this.server = null

            throw e
        }
    }

    fun listShares(): List<String> {
        val session = session
            ?: throw IllegalStateException("Not connected")

        val transport = SMBTransportFactories.SRVSVC.getTransport(session)
        val serverService = ServerService(transport)

        return serverService.shares0
            .filterNotNull()
            .map { it.netName }
    }

    fun list(
        shareName: String,
        path: String = ""
    ): List<SmbEntry> {
        val session = session
            ?: throw IllegalStateException("Not connected")

        val share = session.connectShare(shareName) as DiskShare

        return share.list(path)
            .filter { it.fileName != "." && it.fileName != ".." }
            .map { file ->
                val filePath = if (path.isEmpty()) {
                    file.fileName
                } else {
                    "$path\\${file.fileName}"
                }

                SmbEntry(
                    name = file.fileName,
                    path = filePath,
                    isDirectory = isDirectory(file)
                )
            }
    }

    private fun isDirectory(
        file: FileIdBothDirectoryInformation
    ): Boolean {
        return EnumWithValue.EnumUtils.isSet(
            file.fileAttributes,
            FileAttributes.FILE_ATTRIBUTE_DIRECTORY
        )
    }

    fun disconnect() {
        connection?.close()

        connection = null
        session = null
        server = null

        logger.debug("Disconnected")
    }

    override fun close() {
        disconnect()
        client.close()
    }
}