package com.silversky.core.client

import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbServer

class SmbClient(private val server: SmbServer, private val logger: Logger) : AutoCloseable {
    private val client = SMBClient()
    private var connection: Connection? = null
    private var session: Session? = null;

    fun connect(username: String, password: String, shareName: String) {
        if (connection != null) {
            logger.warn("Tried to connect to connected server ${server.name} (${server.ipAddress})")
            return
        }

        logger.info("Connecting to ${server.name} (${server.ipAddress})")

        try {
            val conn = client.connect(server.ipAddress)
            val authenticationContext =
                AuthenticationContext(username, password.toCharArray(), null)

            session = conn.authenticate(authenticationContext)

            connection = conn
        } catch (e: Exception) {
            logger.error(
                "Failed to connect to share $shareName on " +
                        "${server.name} (${server.ipAddress}): ${e.message}"
            )

            connection?.close()
            connection = null
        }
    }

    private fun isDirectory(file: FileIdBothDirectoryInformation): Boolean {
        return EnumWithValue.EnumUtils.isSet(
            file.fileAttributes,
            FileAttributes.FILE_ATTRIBUTE_DIRECTORY
        )
    }

    fun enumerate(shareName: String): List<SmbEntry> {
        if (session == null) {
            throw IllegalStateException("Not connected to ${server.name}")
        } else {

            val share = session!!.connectShare(shareName) as DiskShare

            return enumerateDirectory(share, "")
        }
    }

    private fun enumerateDirectory(share: DiskShare, path: String): List<SmbEntry> {
        return share.list(path).filter { it.fileName != "." && it.fileName != ".." }.map { file ->
            val isDirectory = isDirectory(file)

            SmbEntry(
                name = file.fileName,
                isDirectory = isDirectory,
                children =
                    if (isDirectory) {
                        val childPath =
                            if (path.isEmpty()) {
                                file.fileName
                            } else {
                                "$path\\${file.fileName}"
                            }

                        enumerateDirectory(share, childPath)
                    } else {
                        emptyList()
                    }
            )
        }
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
