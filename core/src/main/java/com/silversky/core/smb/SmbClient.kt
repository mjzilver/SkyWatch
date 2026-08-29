package com.silversky.core.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.security.jce.JceSecurityProvider
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import com.silversky.core.logger.Logger
import com.silversky.core.model.SmbEntry
import com.silversky.core.model.SmbEntryType
import com.silversky.core.model.SmbFile
import com.silversky.core.model.SmbFileImpl
import com.silversky.core.model.SmbServer
import java.io.InterruptedIOException
import java.util.EnumSet
import java.util.concurrent.ExecutionException

class SmbClient(private val logger: Logger) : AutoCloseable {
  private val client =
      SMBClient(
          SmbConfig.builder()
              .withReadBufferSize(1024 * 1024)
              .withSecurityProvider(JceSecurityProvider())
              .build()
      )
  private val connectionLock = Any()

  var server: SmbServer? = null
    private set

  private var username: String? = null
  private var password: String? = null
  private var connection: Connection? = null
  private var session: Session? = null

  private val shares = mutableMapOf<String, DiskShare>()

  fun connect(
      server: SmbServer,
      username: String,
      password: String,
      isGuest: Boolean = false,
  ) {
    synchronized(connectionLock) {
      if (connection != null && session != null) {
        logger.warn("Already connected")
        return
      }

      connectLocked(
          server = server,
          username = username,
          password = password,
          isGuest = isGuest,
      )
    }
  }

  fun listShares(): List<SmbEntry> {
    return withReconnectRetry {
      synchronized(connectionLock) {
        val session = requireSession()
        val transport = SMBTransportFactories.SRVSVC.getTransport(session)
        val serverService = ServerService(transport)

        serverService.shares1
            .filterNotNull()
            .filter { it.type == 0 }
            .map { share ->
              SmbEntry(
                  name = share.netName,
                  type = SmbEntryType.Share,
                  path = "",
                  shareName = share.netName,
              )
            }
      }
    }
  }

  fun ensureConnected() {
    synchronized(connectionLock) {
      if (connection != null && session != null) {
        return
      }

      val currentServer = server ?: throw IllegalStateException("No server available")
      val currentUsername = username ?: throw IllegalStateException("No username available")
      val currentPassword = password ?: throw IllegalStateException("No password available")

      connectLocked(
          server = currentServer,
          username = currentUsername,
          password = currentPassword,
      )
    }
  }

  fun list(
      shareName: String,
      path: String = "",
  ): List<SmbEntry> {
    return withReconnectRetry {
      synchronized(connectionLock) {
        val share = getShare(shareName)

        share
            .list(path)
            .filter { it.fileName != "." && it.fileName != ".." }
            .map { file ->
              val filePath =
                  if (path.isEmpty()) {
                    file.fileName
                  } else {
                    "$path\\${file.fileName}"
                  }

              SmbEntry(
                  name = file.fileName,
                  path = filePath,
                  type =
                      when {
                        isDirectory(file) -> SmbEntryType.Directory
                        else -> SmbEntryType.File
                      },
                  size = file.endOfFile,
                  dateModified = file.lastWriteTime.toEpochMillis(),
                  isHidden = isHidden(file),
                  shareName = shareName,
              )
            }
      }
    }
  }

  fun listTree(
      shareName: String,
      path: String = "",
  ): List<SmbEntry> {
    return list(shareName, path).map { entry ->
      if (entry.type == SmbEntryType.Directory) {
        entry.copy(children = listTree(shareName, entry.path))
      } else {
        entry
      }
    }
  }

  fun openFile(
      shareName: String,
      path: String,
  ): SmbFile? {
    synchronized(connectionLock) {
      val share = getShare(shareName)

      try {
        if (!share.fileExists(path)) {
          return null
        }

        val file =
            share.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_READ),
                EnumSet.noneOf(FileAttributes::class.java),
                EnumSet.of(
                    SMB2ShareAccess.FILE_SHARE_READ,
                    SMB2ShareAccess.FILE_SHARE_WRITE,
                ),
                SMB2CreateDisposition.FILE_OPEN,
                EnumSet.noneOf(SMB2CreateOptions::class.java),
            )

        logger.debug("SMB FILE OPEN: //$shareName/$path")

        return SmbFileImpl(
            file = file,
            onClose = {
              logger.debug("SMB FILE CLOSE: //$shareName/$path")
            },
        )
      } catch (e: Exception) {
        if (isInterrupted(e)) {
          logger.debug("SMB OPEN CANCELLED: //$shareName/$path")
          Thread.currentThread().interrupt()
          throw InterruptedIOException("SMB open cancelled")
        } else {
          logger.error(
              "SMB FILE OPEN FAILED: //$shareName/$path",
              e,
          )
          throw e
        }
      }
    }
  }

  private fun getShare(
      shareName: String,
  ): DiskShare {
    shares[shareName]?.let { share ->
      if (share.isConnected) {
        return share
      }

      shares.remove(shareName)

      try {
        share.close()
        logger.debug("SMB SHARE CLOSED: //$shareName")
      } catch (_: Exception) {}
    }

    val session = requireSession()

    val share =
        session.connectShare(shareName) as? DiskShare
            ?: throw IllegalStateException("SMB share '$shareName' is not a DiskShare")

    shares[shareName] = share

    logger.debug("SMB SHARE OPEN: //$shareName")

    return share
  }

  private fun <T> withReconnectRetry(
      operation: () -> T,
  ): T {
    var lastException: Exception? = null

    repeat(5) { attempt ->
      try {
        return operation()
      } catch (e: Exception) {
        if (isInterrupted(e)) {
          Thread.currentThread().interrupt()
          throw InterruptedIOException("SMB operation interrupted")
        }

        lastException = e

        if (attempt == 4) {
          logger.error(
              "SMB operation failed after 5 attempts",
              e,
          )
          throw e
        }

        val delay = 200L shl attempt

        logger.warn("SMB operation failed " + "(attempt ${attempt + 1}/5): ${e.message}")

        try {
          Thread.sleep(delay)
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
          throw e
        }

        try {
          reconnect()
        } catch (e: Exception) {
          if (isInterrupted(e)) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("SMB reconnect interrupted")
          }

          lastException = e

          logger.warn("Reconnect failed: ${e.message}")
        }
      }
    }

    throw lastException ?: IllegalStateException("SMB operation failed")
  }

  private fun reconnect() {
    synchronized(connectionLock) {
      val currentServer = server ?: throw IllegalStateException("No server available for reconnect")

      val currentUsername =
          username ?: throw IllegalStateException("No username available for reconnect")
      val currentPassword =
          password ?: throw IllegalStateException("No password available for reconnect")

      invalidateConnectionLocked()

      connectLocked(
          server = currentServer,
          username = currentUsername,
          password = currentPassword,
      )
    }
  }

  private fun connectLocked(
      server: SmbServer,
      username: String,
      password: String,
      isGuest: Boolean = false,
  ) {
    if (connection != null && session != null) {
      return
    }

    var newConnection: Connection? = null

    try {
      newConnection =
          client.connect(
              server.ipAddress,
              server.port,
          )

      val authenticationContext =
          if (isGuest || username == "Everyone") {
            AuthenticationContext.guest()
          } else {
            AuthenticationContext(
                username,
                password.toCharArray(),
                null,
            )
          }

      val newSession = newConnection.authenticate(authenticationContext)

      logger.debug(
          "SMB SERVER: " +
              "maxReadSize=${newConnection.connectionContext.negotiatedProtocol.maxReadSize} " +
              "maxWriteSize=${newConnection.connectionContext.negotiatedProtocol.maxWriteSize} " +
              "maxTransactSize=${newConnection.connectionContext.negotiatedProtocol.maxTransactSize}"
      )

      this.server = server
      this.username = username
      this.password = password
      this.connection = newConnection
      this.session = newSession

      if (server.name == null) {
        server.name = newConnection.connectionContext.server.serverName
      }
    } catch (e: Exception) {
      try {
        newConnection?.close()
      } catch (_: Exception) {}

      connection = null
      session = null

      if (isInterrupted(e)) {
        Thread.currentThread().interrupt()
        throw InterruptedIOException("SMB connect interrupted")
      }

      logger.error(
          "Failed to connect to " + "${server.name ?: server.ipAddress}: ${e.message}",
          e,
      )

      throw e
    }
  }

  private fun invalidateConnectionLocked() {
    shares.forEach { (shareName, share) ->
      try {
        share.close()
        logger.debug("SMB SHARE CLOSED: //$shareName")
      } catch (_: Exception) {}
    }

    shares.clear()

    try {
      connection?.close()
    } catch (_: Exception) {}

    connection = null
    session = null
  }

  private fun requireSession(): Session {
    return session ?: throw IllegalStateException("Not connected")
  }

  private fun isDirectory(
      file: FileIdBothDirectoryInformation,
  ): Boolean {
    return EnumWithValue.EnumUtils.isSet(
        file.fileAttributes,
        FileAttributes.FILE_ATTRIBUTE_DIRECTORY,
    )
  }

  private fun isHidden(
      file: FileIdBothDirectoryInformation,
  ): Boolean {
    val attributes = file.fileAttributes
    return EnumWithValue.EnumUtils.isSet(attributes, FileAttributes.FILE_ATTRIBUTE_HIDDEN) ||
        EnumWithValue.EnumUtils.isSet(attributes, FileAttributes.FILE_ATTRIBUTE_SYSTEM) ||
        file.fileName.startsWith(".")
  }

  private fun isInterrupted(
      throwable: Throwable,
  ): Boolean {
    var current: Throwable? = throwable

    while (current != null) {
      if (current is InterruptedException) {
        return true
      }

      current =
          if (current is ExecutionException && current.cause != null) {
            current.cause
          } else {
            current.cause
          }
    }

    return Thread.currentThread().isInterrupted
  }

  fun disconnect() {
    synchronized(connectionLock) {
      invalidateConnectionLocked()

      server = null
      username = null
      password = null
    }
  }

  override fun close() {
    disconnect()
    client.close()
  }
}
