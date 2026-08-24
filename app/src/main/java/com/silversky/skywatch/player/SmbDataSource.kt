package com.silversky.skywatch.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbFile
import java.io.IOException
import java.io.InterruptedIOException

@UnstableApi
class SmbDataSource(
    private val smbClient: SmbClient,
    private val logger: Logger,
) : BaseDataSource(false) {

  companion object {
    private const val BUFFER_SIZE = 1024 * 1024
  }

  private var file: SmbFile? = null
  private var currentUri: Uri? = null

  private var shareName: String? = null
  private var path: String? = null

  private var position = 0L
  private var remaining = 0L

  private var buffer = ByteArray(BUFFER_SIZE)
  private var bufferPosition = 0
  private var bufferLength = 0

  @Volatile private var closed = false

  override fun open(dataSpec: DataSpec): Long {
    transferInitializing(dataSpec)

    closed = false
    currentUri = dataSpec.uri

    shareName = dataSpec.uri.host ?: throw IllegalArgumentException("SMB URI has no share name")

    path =
        dataSpec.uri.path?.trimStart('/')?.replace('/', '\\')
            ?: throw IllegalArgumentException("SMB URI has no path")

    try {
      file =
          smbClient.openFile(
              shareName = shareName!!,
              path = path!!,
          ) ?: throw IOException("File does not exist: $shareName/$path")
    } catch (e: Exception) {
      if (isInterrupted(e)) {
        logger.debug("SMB OPEN CANCELLED: //$shareName/$path")
        Thread.currentThread().interrupt()
        throw InterruptedIOException("SMB open cancelled")
      }

      if (e is IOException) throw e
      throw IOException("Failed to open SMB file: //$shareName/$path", e)
    }

    position = dataSpec.position

    remaining =
        if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
          (file!!.size - position).coerceAtLeast(0L)
        } else {
          dataSpec.length
        }

    bufferPosition = 0
    bufferLength = 0

    logger.debug("SMB OPEN: //$shareName/$path position=$position remaining=$remaining")

    transferStarted(dataSpec)

    return remaining
  }

  override fun read(
      buffer: ByteArray,
      offset: Int,
      length: Int,
  ): Int {
    if (length == 0) {
      return 0
    }

    if (closed || remaining <= 0L) {
      return C.RESULT_END_OF_INPUT
    }

    var totalRead = 0

    while (totalRead < length && remaining > 0L && !closed) {
      if (bufferPosition >= bufferLength) {
        if (!fillBuffer()) {
          return if (totalRead > 0) {
            totalRead
          } else {
            C.RESULT_END_OF_INPUT
          }
        }

        if (bufferLength <= 0) {
          return if (totalRead > 0) {
            totalRead
          } else {
            C.RESULT_END_OF_INPUT
          }
        }
      }

      val available = bufferLength - bufferPosition
      val requested = length - totalRead

      val toCopy =
          minOf(
              available,
              requested,
              remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
          )

      System.arraycopy(
          this.buffer,
          bufferPosition,
          buffer,
          offset + totalRead,
          toCopy,
      )

      bufferPosition += toCopy
      position += toCopy
      remaining -= toCopy
      totalRead += toCopy

      bytesTransferred(toCopy)
    }

    return totalRead
  }

  private fun fillBuffer(): Boolean {
    if (closed) {
      return false
    }

    val currentFile = file ?: throw IllegalStateException("SMB file is not open")
    val readPosition = position
    val startNanos = System.nanoTime()

    val bytesRead =
        try {
          currentFile.read(
              filePosition = readPosition,
              buffer = buffer,
              bufferOffset = 0,
              length = buffer.size,
          )
        } catch (e: Exception) {
          val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

          if (closed || isInterrupted(e)) {
            logger.debug("SMB READ CANCELLED: position=$readPosition " + "elapsed=${elapsedMs}ms")
            return false
          }

          logger.error(
              "SMB READ FAILED: position=$readPosition " + "elapsed=${elapsedMs}ms",
              e,
          )

          throw e
        }

    if (bytesRead <= 0) {
      return false
    }

    val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
    val speed =
        if (elapsedMs > 0) {
          bytesRead / 1024.0 / 1024.0 / (elapsedMs / 1000.0)
        } else {
          0.0
        }

    logger.debug(
        "SMB READ: position=$readPosition " +
            "bytes=$bytesRead " +
            "elapsed=${elapsedMs}ms " +
            "speed=%.2f MB/s".format(speed)
    )

    bufferPosition = 0
    bufferLength = bytesRead

    return true
  }

  private fun isInterrupted(error: Throwable): Boolean {
    var current: Throwable? = error

    while (current != null) {
      if (current is InterruptedException) {
        return true
      }

      current = current.cause
    }

    return false
  }

  override fun getUri(): Uri? = currentUri

  override fun close() {
    if (closed) {
      return
    }

    closed = true

    logger.debug("SMB CLOSE: //$shareName/$path")

    try {
      file?.close()
    } catch (_: Exception) {}

    file = null
    bufferPosition = 0
    bufferLength = 0

    transferEnded()
  }
}
