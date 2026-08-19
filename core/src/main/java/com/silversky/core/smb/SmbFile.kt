package com.silversky.core.smb

import com.hierynomus.smbj.share.File

interface SmbFile : AutoCloseable {
  val size: Long

  fun read(
      filePosition: Long,
      buffer: ByteArray,
      bufferOffset: Int,
      length: Int,
  ): Int
}

class SmbFileImpl(
    private val file: File,
    private val onClose: () -> Unit,
) : SmbFile {

  private var closed = false

  override val size: Long
    get() {
      check(!closed) { "SmbFile is closed" }
      return file.fileInformation.standardInformation.endOfFile
    }

  override fun read(
      filePosition: Long,
      buffer: ByteArray,
      bufferOffset: Int,
      length: Int,
  ): Int {
    check(!closed) { "SmbFile is closed" }

    return file.read(
        buffer,
        filePosition,
        bufferOffset,
        length,
    )
  }

  override fun close() {
    if (closed) {
      return
    }

    closed = true

    try {
      file.close()
    } finally {
      onClose()
    }
  }
}
