package com.silversky.skywatch.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.silversky.core.client.SmbClient
import com.silversky.core.smb.SmbFile

class SmbDataSource(private val smbClient: SmbClient) : BaseDataSource(false) {

  private var file: SmbFile? = null
  private var currentUri: Uri? = null

  private var shareName: String? = null
  private var path: String? = null

  private var position = 0L
  private var remaining = 0L

  override fun open(dataSpec: DataSpec): Long {
    transferInitializing(dataSpec)

    currentUri = dataSpec.uri

    shareName = dataSpec.uri.host ?: throw IllegalArgumentException("SMB URI has no share name")

    path =
        dataSpec.uri.path?.trimStart('/')?.replace('/', '\\')
            ?: throw IllegalArgumentException("SMB URI has no path")

    val openedFile =
        smbClient.openFile(
            shareName = shareName!!,
            path = path!!,
        ) ?: throw IllegalStateException("File does not exist: $shareName/$path")

    file = openedFile

    position = dataSpec.position

    remaining =
        if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
          openedFile.size - position
        } else {
          dataSpec.length
        }

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

    if (remaining == 0L) {
      return C.RESULT_END_OF_INPUT
    }

    val requested =
        minOf(
                length.toLong(),
                remaining,
            )
            .toInt()

    val currentFile = file ?: throw IllegalStateException("SMB file is not open")

    val bytesRead =
        try {
          currentFile.read(
              filePosition = position,
              buffer = buffer,
              bufferOffset = offset,
              length = requested,
          )
        } catch (e: Exception) {
          val share =
              shareName
                  ?: throw IllegalStateException(
                      "SMB share is not set",
                      e,
                  )

          val filePath =
              path
                  ?: throw IllegalStateException(
                      "SMB path is not set",
                      e,
                  )

          file =
              smbClient.openFile(
                  shareName = share,
                  path = filePath,
              )
                  ?: throw IllegalStateException(
                      "File disappeared: $share/$filePath",
                      e,
                  )

          file!!.read(
              filePosition = position,
              buffer = buffer,
              bufferOffset = offset,
              length = requested,
          )
        }

    if (bytesRead <= 0) {
      return C.RESULT_END_OF_INPUT
    }

    position += bytesRead
    remaining -= bytesRead

    bytesTransferred(bytesRead)

    return bytesRead
  }

  override fun getUri(): Uri? = currentUri

  override fun close() {
    try {
      file?.close()
    } catch (_: Exception) {}

    file = null

    transferEnded()
  }
}
