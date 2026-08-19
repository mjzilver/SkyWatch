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
import com.silversky.core.smb.RefCountedDiskShare
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbFile
import com.silversky.core.smb.SmbFileImpl
import com.silversky.core.smb.SmbServer
import java.util.EnumSet

class SmbClient(private val logger: Logger) : AutoCloseable {
  private val client = SMBClient()
  private val connectionLock = Any()

  var server: SmbServer? = null
    private set

  private var username: String? = null
  private var password: String? = null
  private var connection: Connection? = null
  private var session: Session? = null

  private val shares = mutableMapOf<String, RefCountedDiskShare>()

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

      logger.info(
          "Connecting to ${server.name ?: server.ipAddress} " +
              "(${server.ipAddress}:${server.port})"
      )

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

        this.server = server
        this.username = username
        this.password = password
        this.connection = newConnection
        this.session = newSession

        if (server.name == null) {
          server.name = newConnection.connectionContext.server.serverName
        }

        logger.info("Connected to ${server.name ?: server.ipAddress} as $username")
      } catch (e: Exception) {
        try {
          newConnection?.close()
        } catch (_: Exception) {}

        connection = null
        session = null

        logger.error(
            "Failed to connect to " + "${server.name ?: server.ipAddress}: ${e.message}",
            e,
        )

        throw e
      }
    }
  }

  fun listShares(): List<String> {
    return withReconnectRetry {
      synchronized(connectionLock) {
        val session = requireSession()

        val transport = SMBTransportFactories.SRVSVC.getTransport(session)

        val serverService = ServerService(transport)

        serverService.shares1
            .filterNotNull()
            .filter { share ->
              share.type == 0
            }
            .map { it.netName }
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
        val session = requireSession()

        val share = session.connectShare(shareName) as? DiskShare ?: return@synchronized emptyList()

        try {
          share
              .list(path)
              .filter {
                it.fileName != "." && it.fileName != ".."
              }
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
                    isDirectory = isDirectory(file),
                )
              }
        } finally {
          try {
            share.close()
          } catch (_: Exception) {}
        }
      }
    }
  }

  fun openFile(
      shareName: String,
      path: String,
  ): SmbFile? {
    synchronized(connectionLock) {
      val session = requireSession()
      val sharedShare = getShare(session, shareName)

      try {
        if (!sharedShare.share.fileExists(path)) {
          return null
        }

        val file =
            sharedShare.share.openFile(
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

        sharedShare.acquire()

        logger.debug(
            "SMB FILE OPEN: //$shareName/$path " + "shareRefs=${sharedShare.referenceCount()}"
        )

        return SmbFileImpl(
            file = file,
            onClose = {
              releaseShare(
                  shareName = shareName,
                  sharedShare = sharedShare,
              )
            },
        )
      } catch (e: Exception) {
        cleanupUnusedShare(
            shareName = shareName,
            sharedShare = sharedShare,
        )

        throw e
      }
    }
  }

  private fun getShare(
      session: Session,
      shareName: String,
  ): RefCountedDiskShare {
    val existing = shares[shareName]

    if (existing != null && !existing.isClosed()) {
      return existing
    }

    if (existing != null) {
      shares.remove(shareName)
    }

    val diskShare =
        session.connectShare(shareName) as? DiskShare
            ?: throw IllegalStateException("SMB share '$shareName' is not a DiskShare")

    val wrapped =
        RefCountedDiskShare(
            shareName = shareName,
            share = diskShare,
        )

    shares[shareName] = wrapped

    logger.debug("SMB SHARE OPEN: //$shareName")

    return wrapped
  }

  private fun releaseShare(
      shareName: String,
      sharedShare: RefCountedDiskShare,
  ) {
    synchronized(connectionLock) {
      sharedShare.release()

      val references = sharedShare.referenceCount()

      logger.debug("SMB SHARE RELEASE: //$shareName refs=$references")

      if (references == 0) {
        if (shares[shareName] === sharedShare) {
          shares.remove(shareName)
        }

        logger.debug("SMB SHARE CLOSE: //$shareName")
      }
    }
  }

  private fun cleanupUnusedShare(
      shareName: String,
      sharedShare: RefCountedDiskShare,
  ) {
    synchronized(connectionLock) {
      if (sharedShare.referenceCount() != 0) {
        return
      }

      if (shares[shareName] === sharedShare) {
        shares.remove(shareName)
      }

      sharedShare.close()

      logger.debug("SMB SHARE CLOSE AFTER FAILED OPEN: //$shareName")
    }
  }

  private fun <T> withReconnectRetry(
      operation: () -> T,
  ): T {
    var lastException: Exception? = null

    repeat(5) { attempt ->
      try {
        return operation()
      } catch (e: Exception) {
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

        Thread.sleep(delay)

        try {
          reconnect()
        } catch (e: Exception) {
          lastException = e

          logger.warn("Reconnect failed: ${e.message}")
        }
      }
    }

    throw lastException ?: IllegalStateException("SMB operation failed")
  }

  private fun reconnect() {
    synchronized(connectionLock) {
      if (shares.values.any { it.referenceCount() > 0 }) {
        throw IllegalStateException("Cannot reconnect while SMB files are active")
      }

      val currentServer = server ?: throw IllegalStateException("No server available for reconnect")

      val currentUsername =
          username ?: throw IllegalStateException("No username available for reconnect")

      val currentPassword =
          password ?: throw IllegalStateException("No password available for reconnect")

      logger.info("Reconnecting to ${currentServer.name ?: currentServer.ipAddress}")

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
  ) {
    if (connection != null && session != null) {
      return
    }

    logger.info(
        "Connecting to ${server.name ?: server.ipAddress} " + "(${server.ipAddress}:${server.port})"
    )

    var newConnection: Connection? = null

    try {
      newConnection =
          client.connect(
              server.ipAddress,
              server.port,
          )

      val authenticationContext =
          AuthenticationContext(
              username,
              password.toCharArray(),
              null,
          )

      val newSession = newConnection.authenticate(authenticationContext)

      this.server = server
      this.username = username
      this.password = password
      this.connection = newConnection
      this.session = newSession

      logger.info("Connected to ${server.name ?: server.ipAddress} as $username")
    } catch (e: Exception) {
      try {
        newConnection?.close()
      } catch (_: Exception) {}

      connection = null
      session = null

      logger.error(
          "Failed to connect to " + "${server.name ?: server.ipAddress}: ${e.message}",
          e,
      )

      throw e
    }
  }

  private fun invalidateConnectionLocked() {
    shares.values.forEach {
      try {
        it.close()
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

  fun disconnect() {
    synchronized(connectionLock) {
      shares.values.forEach {
        try {
          it.close()
        } catch (_: Exception) {}
      }

      shares.clear()

      try {
        connection?.close()
      } catch (_: Exception) {}

      connection = null
      session = null
      server = null
      username = null
      password = null

      logger.debug("Disconnected")
    }
  }

  override fun close() {
    disconnect()
    client.close()
  }
}
