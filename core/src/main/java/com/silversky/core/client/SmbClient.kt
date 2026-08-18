package com.silversky.core.client

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
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
import com.silversky.core.smb.SmbFile
import com.silversky.core.smb.SmbFileImpl
import com.silversky.core.smb.SmbServer
import java.util.EnumSet

class SmbClient(
    private val logger: Logger
) : AutoCloseable {

    private val client = SMBClient()

    var server: SmbServer? = null
        private set

    private var username: String? = null
    private var password: String? = null

    private var connection: Connection? = null
    private var session: Session? = null

    fun connect(
        server: SmbServer,
        username: String,
        password: String
    ) {
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

            val authenticationContext = AuthenticationContext(
                username,
                password.toCharArray(),
                null
            )

            val authenticatedSession = conn.authenticate(authenticationContext)

            this.server = server
            this.username = username
            this.password = password
            this.connection = conn
            this.session = authenticatedSession

            if (server.name == null) {
                server.name = getServerName()
            }

            logger.info(
                "Connected to ${server.name ?: server.ipAddress} as $username"
            )
        } catch (e: Exception) {
            connection?.close()
            connection = null
            session = null
            this.server = null

            logger.error(
                "Failed to connect to " +
                        "${server.name ?: server.ipAddress}: ${e.message}"
            )

            throw e
        }
    }

    fun listShares(): List<String> {
        return withReconnectRetry {
            val session = session
                ?: throw IllegalStateException("Not connected")

            val transport = SMBTransportFactories.SRVSVC.getTransport(session)
            val serverService = ServerService(transport)

            serverService.shares0
                .filterNotNull()
                .map { it.netName }
        }
    }

    fun getServerName(): String? {
        val connection = connection
            ?: throw IllegalStateException("Not connected")

        return connection.connectionContext.server.serverName
    }

    fun list(
        shareName: String,
        path: String = ""
    ): List<SmbEntry> {
        return withReconnectRetry {
            val session = session
                ?: throw IllegalStateException("Not connected")

            val share = session.connectShare(shareName) as? DiskShare
                ?: return@withReconnectRetry emptyList()

            share.list(path)
                .filter {
                    it.fileName != "." && it.fileName != ".."
                }
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
    }

    fun openFile(
        shareName: String,
        path: String
    ): SmbFile? {
        return withReconnectRetry {
            val session = session
                ?: throw IllegalStateException("Not connected")

            val share = session.connectShare(shareName) as DiskShare

            if (!share.fileExists(path)) {
                return@withReconnectRetry null
            }

            val file = share.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_READ),
                EnumSet.noneOf(FileAttributes::class.java),
                EnumSet.of(
                    SMB2ShareAccess.FILE_SHARE_READ,
                    SMB2ShareAccess.FILE_SHARE_WRITE
                ),
                SMB2CreateDisposition.FILE_OPEN,
                EnumSet.noneOf(SMB2CreateOptions::class.java)
            )

            SmbFileImpl(file)
        }
    }

    private fun <T> withReconnectRetry(
        operation: () -> T
    ): T {
        try {
            return operation()
        } catch (e: Exception) {
            logger.warn(
                "SMB operation failed: ${e.message}. Attempting reconnect."
            )
        }

        reconnect()

        return operation()
    }

    private fun reconnect() {
        val server = server
            ?: throw IllegalStateException("No server available for reconnect")

        val username = username
            ?: throw IllegalStateException("No username available for reconnect")

        val password = password
            ?: throw IllegalStateException("No password available for reconnect")

        logger.info(
            "Reconnecting to ${server.name ?: server.ipAddress}"
        )

        invalidateConnection()

        connect(
            server = server,
            username = username,
            password = password
        )
    }

    private fun invalidateConnection() {
        try {
            connection?.close()
        } catch (_: Exception) {
        }

        connection = null
        session = null
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
        username = null
        password = null

        logger.debug("Disconnected")
    }

    override fun close() {
        disconnect()
        client.close()
    }
}